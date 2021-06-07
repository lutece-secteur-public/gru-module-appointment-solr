/*
 * Copyright (c) 2002-2021, City of Paris
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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.solr.client.solrj.SolrServerException;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.rule.ReservationRule;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.ReservationRuleService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.listeners.IFormListener;
import fr.paris.lutece.plugins.appointment.service.listeners.ISlotListener;
import fr.paris.lutece.plugins.appointment.service.listeners.IWeekDefinitionListener;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Appointment listeners for Solr
 * 
 * @author Laurent Payen
 *
 */
public class SolrAppointmentListener implements IFormListener, ISlotListener, IWeekDefinitionListener
{
    private static ConcurrentMap<Integer, AtomicBoolean> _lockIndexerIsRuning = new ConcurrentHashMap<>( );
    private static ConcurrentMap<Integer, AtomicBoolean> _lockIndexToLunch = new ConcurrentHashMap<>( );
    private static Queue<Slot> _queueSlotToIndex = new ConcurrentLinkedQueue<>( );
    private static AtomicBoolean _bIndexIsRunning = new AtomicBoolean( false );

    /**
     * Reindex the form and the slots in solr
     * 
     * @param nIdForm
     *            the form id
     */
    private void reindexForm( final int nIdForm )
    {
        AtomicBoolean bIndexIsRunning = getIndexRuningLock( nIdForm );
        AtomicBoolean bIndexToLunch = getIndexToLunchLock( nIdForm );
        bIndexToLunch.set( true );
        if ( bIndexIsRunning.compareAndSet( false, true ) )
        {
            ( new Thread( )
            {
                @Override
                public void run( )
                {
                    StringBuilder sbLogs = new StringBuilder( );
                    try
                    {
                        SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
                        sbLogs = new StringBuilder( );
                        while ( bIndexToLunch.compareAndSet( true, false ) )
                        {
                            AppointmentFormDTO appointmentForm = FormService.buildAppointmentFormWithoutReservationRule( nIdForm );
                            solrAppointmentIndexer.deleteFormAndListSlots( nIdForm, sbLogs );
                            if ( appointmentForm.getIsActive( ) )
                            {
                                solrAppointmentIndexer.writeFormAndListSlots( appointmentForm, sbLogs );
                            }
                        }
                    }
                    catch( IOException | SolrServerException e )
                    {
                        AppLogService.error( "Error during SolrAppointmentListener reindexForm: " + sbLogs, e );
                    }
                    finally
                    {
                        bIndexIsRunning.set( false );
                    }
                }
            } ).start( );
        }
    }

    /**
     * Reindex the slot (and the related form to have the good number of available places) in solr
     * 
     * @param nIdSlot
     *            the slot id
     */
    private void reindexSlot( Slot slot )
    {
        if ( _bIndexIsRunning.compareAndSet( false, true ) )
        {

            ( new Thread( )
            {
                @Override
                public void run( )
                {
                    StringBuilder sbLogs = new StringBuilder( );
                    try
                    {
                        SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
                        solrAppointmentIndexer.writeSlotAndForm( slot, sbLogs, _queueSlotToIndex );

                    }
                    catch( IOException e )
                    {
                        AppLogService.error( "Error during SolrAppointmentListener reindexSlot: " + sbLogs, e );
                    }
                    finally
                    {
                        _bIndexIsRunning.set( false );
                        if ( !_queueSlotToIndex.isEmpty( ) )
                        {

                            reindexSlot( _queueSlotToIndex.poll( ) );
                        }
                    }
                }
            } ).start( );
        }
        else
        {

            _queueSlotToIndex.add( slot );
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
            SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
            solrAppointmentIndexer.deleteFormAndListSlots( nIdForm, sbLogs );
        }
        catch( IOException | SolrServerException e )
        {
            AppLogService.error( "Error during SolrAppointmentListener deleteForm: " + sbLogs, e );
        }
    }

    /**
     * Delete the slot in solr
     * 
     * @param slot
     *            the slot to delete
     */
    private void deleteSlot( Slot slot )
    {
        StringBuilder sbLogs = new StringBuilder( );
        try
        {
            SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
            solrAppointmentIndexer.deleteSlot( slot, sbLogs );
        }
        catch( IOException | SolrServerException e )
        {
            AppLogService.error( "Error during SolrAppointmentListener deleteSlot: " + sbLogs, e );
        }
    }

    private static synchronized AtomicBoolean getIndexRuningLock( int nkey )
    {
        _lockIndexerIsRuning.putIfAbsent( nkey, new AtomicBoolean( false ) );
        return _lockIndexerIsRuning.get( nkey );
    }

    private static synchronized AtomicBoolean getIndexToLunchLock( int nkey )
    {
        _lockIndexToLunch.putIfAbsent( nkey, new AtomicBoolean( false ) );
        return _lockIndexToLunch.get( nkey );
    }

    @Override
    public void notifySlotChange( int nIdSlot )
    {
        Slot slot = SlotService.findSlotById( nIdSlot );
        reindexSlot( slot );
    }

    @Override
    public void notifySlotCreation( int nIdSlot )
    {
        notifySlotChange( nIdSlot );
    }

    @Override
    public void notifySlotRemoval( Slot slot )
    {
        if ( FormUtil.isPeriodValidToIndex( slot.getIdForm( ), slot.getDate( ), slot.getDate( ) ) )
        {
            // deleteSlot( slot );
            reindexForm( slot.getIdForm( ) );
        }
    }

    @Override
    public void notifySlotEndingTimeHasChanged( int nIdSlot, int nIdFom, LocalDateTime endingDateTime )
    {

        if ( FormUtil.isPeriodValidToIndex( nIdFom, endingDateTime.toLocalDate( ), endingDateTime.toLocalDate( ) ) )
        {

            reindexForm( nIdFom );
        }

    }

    @Override
    public void notifyFormChange( int nIdForm )
    {
        reindexForm( nIdForm );
    }

    @Override
    public void notifyFormCreation( int nIdForm )
    {
        reindexForm( nIdForm );
    }

    @Override
    public void notifyFormRemoval( int nIdForm )
    {
        deleteForm( nIdForm );
    }

    @Override
    public void notifyWeekAssigned( WeekDefinition week )
    {

        ReservationRule rule = ReservationRuleService.findReservationRuleById( week.getIdReservationRule( ) );
        if ( FormUtil.isPeriodValidToIndex( rule.getIdForm( ), week.getDateOfApply( ), week.getEndingDateOfApply( ) ) )
        {

            reindexForm( rule.getIdForm( ) );
        }

    }

    @Override
    public void notifyWeekUnassigned( WeekDefinition week )
    {

        notifyWeekAssigned( week );
    }

    @Override
    public void notifyListWeeksChanged( int nIdForm, List<WeekDefinition> listWeek )
    {

        WeekDefinition weekWithDateMin = listWeek.stream( ).min( Comparator.comparing( WeekDefinition::getDateOfApply ) ).orElse( null );
        WeekDefinition weekWithDateMax = listWeek.stream( ).max( Comparator.comparing( WeekDefinition::getEndingDateOfApply ) ).orElse( null );
        if ( weekWithDateMin != null && weekWithDateMax != null
                && FormUtil.isPeriodValidToIndex( nIdForm, weekWithDateMin.getDateOfApply( ), weekWithDateMax.getEndingDateOfApply( ) ) )
        {

            reindexForm( nIdForm );
        }
    }

}
