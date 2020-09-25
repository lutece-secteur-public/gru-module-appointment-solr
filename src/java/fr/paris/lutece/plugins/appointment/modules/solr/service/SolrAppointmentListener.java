/*
 * Copyright (c) 2002-2018, Mairie de Paris
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

import org.apache.solr.client.solrj.SolrServerException;

import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinitionHome;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.service.FormService;
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

    /**
     * Reindex the form and the slots in solr
     * 
     * @param nIdForm
     *            the form id
     */
    private void reindexForm( final int nIdForm )
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
                    solrAppointmentIndexer.deleteFormAndListSlots( nIdForm, sbLogs );
                    sbLogs = new StringBuilder( );
                    AppointmentFormDTO appointmentForm = FormService.buildAppointmentForm( nIdForm, 0, 0 );
                    solrAppointmentIndexer.writeFormAndListSlots( appointmentForm, sbLogs );
                }
                catch( IOException | SolrServerException e )
                {
                    AppLogService.error( "Error during SolrAppointmentListener reindexForm: " + sbLogs, e );
                }
            }
        } ).start( );
    }

    /**
     * Reindex the slot (and the related form to have the good number of available places) in solr
     * 
     * @param nIdSlot
     *            the slot id
     */
    private void reindexSlot( Slot slot )
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
                    solrAppointmentIndexer.writeSlotAndForm( slot, sbLogs );
                }
                catch( IOException e )
                {
                    AppLogService.error( "Error during SolrAppointmentListener reindexSlot: " + sbLogs, e );
                }
            }
        } ).start( );
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
        StringBuffer sbLogs = new StringBuffer( );
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

    @Override
    public void notifyWeekDefinitionChange( int nIdWeekDefinition )
    {
        WeekDefinition weekDefinition = WeekDefinitionHome.findByPrimaryKey( nIdWeekDefinition );
        reindexForm( weekDefinition.getIdForm( ) );
    }

    @Override
    public void notifyWeekDefinitionCreation( int nIdWeekDefinition )
    {
        WeekDefinition weekDefinition = WeekDefinitionHome.findByPrimaryKey( nIdWeekDefinition );
        reindexForm( weekDefinition.getIdForm( ) );
    }

    @Override
    public void notifyWeekDefinitionRemoval( int nIdForm )
    {
        reindexForm( nIdForm );
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
        Slot slot = SlotService.findSlotById( nIdSlot );
        reindexForm( slot.getIdForm( ) );
    }

    @Override
    public void notifySlotRemoval( int nIdSlot )
    {
        // The listener is called before the actual deletion, so we can get the
        // slot.
        Slot slot = SlotService.findSlotById( nIdSlot );
        deleteSlot( slot );
        reindexForm( slot.getIdForm( ) );
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
}
