/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.appointment.modules.solr.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.rule.ReservationRule;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.ReservationRuleService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.event.AppointmentFormRemovalEvent;
import fr.paris.lutece.plugins.appointment.service.event.FormEvent;
import fr.paris.lutece.plugins.appointment.service.event.SlotEndingTimeChangedEvent;
import fr.paris.lutece.plugins.appointment.service.event.SlotEvent;
import fr.paris.lutece.plugins.appointment.service.event.WeekDefinitionEvent;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

/**
 * Appointment listeners for Solr.
 * All observer methods use {@code @ObservesAsync} because the appointment plugin
 * fires all events with {@code fireAsync()}. The CDI container handles the async
 * execution — no manual threading needed.
 *
 * @author Laurent Payen
 *
 */
@ApplicationScoped
public class SolrAppointmentListener
{
    @Inject
    private SolrAppointmentIndexer _solrAppointmentIndexer;

    /**
     * Reindex the form and the slots in solr
     *
     * @param nIdForm
     *            the form id
     */
    private void reindexForm( int nIdForm )
    {
        StringBuilder sbLogs = new StringBuilder( );
        try
        {
            AppointmentFormDTO appointmentForm = FormService.buildAppointmentFormWithoutReservationRule( nIdForm );
            _solrAppointmentIndexer.deleteFormAndListSlots( nIdForm, sbLogs );
            if ( appointmentForm.getIsActive( ) )
            {
                _solrAppointmentIndexer.writeFormAndListSlots( appointmentForm, sbLogs );
            }
        }
        catch( IOException | SolrServerException e )
        {
            AppLogService.error( "Error during SolrAppointmentListener reindexForm: {}", sbLogs, e );
        }
    }

    /**
     * Reindex the slot (and the related form to have the good number of available places) in solr
     *
     * @param slot
     *            the slot
     */
    private void reindexSlot( Slot slot )
    {
        StringBuilder sbLogs = new StringBuilder( );
        try
        {
            _solrAppointmentIndexer.writeSlotAndForm( slot, sbLogs );
        }
        catch( IOException e )
        {
            AppLogService.error( "Error during SolrAppointmentListener reindexSlot: {}", sbLogs, e );
        }
    }

    /**
     * Delete the form and all its slots in solr
     *
     * @param nIdForm
     *            the form id
     */
    private void deleteForm( int nIdForm )
    {
        StringBuilder sbLogs = new StringBuilder( );
        try
        {
            _solrAppointmentIndexer.deleteFormAndListSlots( nIdForm, sbLogs );
        }
        catch( IOException | SolrServerException e )
        {
            AppLogService.error( "Error during SolrAppointmentListener deleteForm: {}", sbLogs, e );
        }
    }

    /**
     * Observes slot creation and change events
     *
     * @param event
     *            the slot event
     */
    public void onSlotChanged( @ObservesAsync SlotEvent event )
    {
        if ( event.getSlot( ) != null )
        {
            Slot slot = event.getSlot( );
            if ( FormUtil.isPeriodValidToIndex( slot.getIdForm( ), slot.getDate( ), slot.getDate( ) ) )
            {
                reindexForm( slot.getIdForm( ) );
            }
        }
        else
        {
            Slot slot = SlotService.findSlotById( event.getIdSlot( ) );
            reindexSlot( slot );
        }
    }

    /**
     * Observes slot ending time change events
     *
     * @param event
     *            the slot ending time changed event
     */
    public void onSlotEndingTimeChanged( @ObservesAsync SlotEndingTimeChangedEvent event )
    {
        if ( FormUtil.isPeriodValidToIndex( event.getIdForm( ), event.getEndingDateTime( ).toLocalDate( ), event.getEndingDateTime( ).toLocalDate( ) ) )
        {
            reindexForm( event.getIdForm( ) );
        }
    }

    /**
     * Observes form creation and change events
     *
     * @param event
     *            the form event
     */
    public void onFormChanged( @ObservesAsync FormEvent event )
    {
        reindexForm( event.getIdForm( ) );
    }

    /**
     * Observes form removal events
     *
     * @param event
     *            the form removal event
     */
    public void onFormRemoved( @ObservesAsync AppointmentFormRemovalEvent event )
    {
        deleteForm( event.getIdAppointmentForm( ) );
    }

    /**
     * Observes week definition events (assigned, unassigned, list changed)
     *
     * @param event
     *            the week definition event
     */
    public void onWeekDefinitionChanged( @ObservesAsync WeekDefinitionEvent event )
    {
        if ( event.getWeekDefinition( ) != null )
        {
            WeekDefinition week = event.getWeekDefinition( );
            ReservationRule rule = ReservationRuleService.findReservationRuleById( week.getIdReservationRule( ) );
            if ( FormUtil.isPeriodValidToIndex( rule.getIdForm( ), week.getDateOfApply( ), week.getEndingDateOfApply( ) ) )
            {
                reindexForm( rule.getIdForm( ) );
            }
        }
        else if ( event.getListWeekDefinition( ) != null )
        {
            List<WeekDefinition> listWeek = event.getListWeekDefinition( );
            int nIdForm = event.getIdForm( );
            WeekDefinition weekWithDateMin = listWeek.stream( ).min( Comparator.comparing( WeekDefinition::getDateOfApply ) ).orElse( null );
            WeekDefinition weekWithDateMax = listWeek.stream( ).max( Comparator.comparing( WeekDefinition::getEndingDateOfApply ) ).orElse( null );
            if ( weekWithDateMin != null && weekWithDateMax != null
                    && FormUtil.isPeriodValidToIndex( nIdForm, weekWithDateMin.getDateOfApply( ), weekWithDateMax.getEndingDateOfApply( ) ) )
            {
                reindexForm( nIdForm );
            }
        }
    }
}
