/*
 * Copyright (c) 2002-2015, Mairie de Paris
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

import fr.paris.lutece.plugins.appointment.business.AppointmentForm;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinitionHome;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.business.slot.SlotHome;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.listeners.IFormListener;
import fr.paris.lutece.plugins.appointment.service.listeners.ISlotListener;
import fr.paris.lutece.plugins.appointment.service.listeners.IWeekDefinitionListener;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;

public class SolrAppointmentListener implements IFormListener, ISlotListener, IWeekDefinitionListener
{

    /**
     * The form must exist in the database
     */
    private void reindexForm( final int nIdForm )
    {
        ( new Thread( )
        {
            @Override
            public void run( )
            {
                StringBuffer sbLogs = new StringBuffer( );
                try
                {
                    SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
                    solrAppointmentIndexer.deleteAppointmentFormAndSlots( nIdForm, sbLogs );
                    AppointmentForm appointmentForm = FormService.buildAppointmentForm( nIdForm, 0, 0 );
                    solrAppointmentIndexer.writeAppointmentFormAndSlots( appointmentForm, sbLogs );
                }
                catch( Exception e )
                {
                    AppLogService.error( "Error during SolrAppointmentListener reindexForm: " + sbLogs, e );
                }
            }
        } ).start( );
    }

    /**
     * The slot must exist in the database
     */
    private void reindexSlot( int nIdSlot )
    {
        StringBuffer sbLogs = new StringBuffer( );
        try
        {
            SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
            solrAppointmentIndexer.writeAppointmentSlot( nIdSlot, sbLogs );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error during SolrAppointmentListener reindexSlot: " + sbLogs, e );
        }
    }

    private void deleteForm( int nIdForm )
    {
        StringBuffer sbLogs = new StringBuffer( );
        try
        {
            SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
            solrAppointmentIndexer.deleteAppointmentFormAndSlots( nIdForm, sbLogs );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error during SolrAppointmentListener deleteForm: " + sbLogs, e );
        }
    }

    private void deleteSlot( Slot slot )
    {
        StringBuffer sbLogs = new StringBuffer( );
        try
        {
            SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( SolrAppointmentIndexer.BEAN_NAME );
            solrAppointmentIndexer.deleteSlot( slot, sbLogs );
        }
        catch( Exception e )
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
    public void notifyWeekDefinitionRemoval( int nIdWeekDefinition )
    {
        // The listener is called before the actual deletion, so we can get the form id.
        WeekDefinition weekDefinition = WeekDefinitionHome.findByPrimaryKey( nIdWeekDefinition );
        reindexForm( weekDefinition.getIdForm( ) );
    }

    @Override
    public void notifySlotChange( int nIdSlot )
    {
        reindexSlot( nIdSlot );
    }

    @Override
    public void notifySlotCreation( int nIdSlot )
    {
        reindexSlot( nIdSlot );
    }

    @Override
    public void notifySlotRemoval( int nIdSlot )
    {
        // The listener is called before the actual deletion, so we can get the slot.
        Slot appointmentSlot = SlotHome.findByPrimaryKey( nIdSlot );
        deleteSlot( appointmentSlot );
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
