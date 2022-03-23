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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.plugins.search.solr.business.SolrServerService;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.portal.service.search.SearchItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

/**
 * Indexer of the slots and forms of RDV V2
 * 
 * @author Laurent Payen
 *
 */
public class SolrAppointmentIndexer implements SolrIndexer
{

    public static final String BEAN_NAME = "appointment-solr.solrAppointmentIndexer";

    private static ConcurrentMap<String, Object> _lockIndexer = new ConcurrentHashMap<>( );

    @Override
    public List<String> indexDocuments( )
    {
        List<String> errors = new ArrayList<>( );
        for ( AppointmentFormDTO appointmentForm : FormService.buildAllActiveAppointmentForm( ) )
        {
            try
            {
                writeFormAndListSlots( appointmentForm );
            }
            catch( IOException e )
            {
                AppLogService.error( "Error indexing AppointmentForm" + appointmentForm.getIdForm( ), e );
                errors.add( e.toString( ) );
            }
        }
        return errors;
    }

    @Override
    public String getResourceUid( String strResourceId, String strResourceType )
    {
        StringBuilder stringBuilder = new StringBuilder( strResourceId );
        if ( Utilities.RESOURCE_TYPE_SLOT.equals( strResourceType ) )
        {
            stringBuilder.append( '_' ).append( Utilities.SHORT_NAME_SLOT );
        }
        else
            if ( Utilities.RESOURCE_TYPE_APPOINTMENT.equals( strResourceType ) )
            {
                stringBuilder.append( '_' ).append( Utilities.SHORT_NAME_APPOINTMENT );
            }
            else
            {
                AppLogService.error( "SolrAppointmentIndexer, unknown resourceType: " + strResourceType );
                return null;
            }
        return stringBuilder.toString( );
    }

    @Override
    public List<Field> getAdditionalFields( )
    {
        return new ArrayList<>( );
    }

    @Override
    public String getDescription( )
    {
        return Utilities.APPOINTMENT_DESCRIPTION;
    }

    @Override
    public List<SolrItem> getDocuments( String arg0 )
    {
        return new ArrayList<>( );
    }

    @Override
    public String getName( )
    {
        return Utilities.APPOINTMENT_FORM_NAME;
    }

    @Override
    public List<String> getResourcesName( )
    {
        return new ArrayList<>( );
    }

    @Override
    public String getVersion( )
    {
        return Utilities.APPOINTMENT_VERSION;
    }

    @Override
    public boolean isEnable( )
    {
        return Boolean.valueOf( AppPropertiesService.getProperty( Utilities.PROPERTY_INDEXER_ENABLE ) );
    }

    /**
     * Write the Appointment Form and all the slots of this form to Solr
     * 
     * @param appointmentForm
     *            the appointment form
     * @throws IOException
     */
    public void writeFormAndListSlots( AppointmentFormDTO appointmentForm ) throws IOException
    {
        writeFormAndListSlots( appointmentForm, SolrIndexerService.getSbLogs( ) );
    }

    /**
     * Write the Appointment Form and all the related slots to Solr
     * 
     * @param appointmentForm
     *            the Appointment Form
     * @param sbLogs
     *            the logs
     * @throws IOException
     */
    public void writeFormAndListSlots( AppointmentFormDTO appointmentForm, StringBuilder sbLogs ) throws IOException
    {
        Object lock = getLock( Utilities.buildResourceUid( Integer.toString( appointmentForm.getIdForm( ) ), Utilities.RESOURCE_TYPE_APPOINTMENT ) );
        synchronized( lock )
        {
            List<Slot> listAllSlots = SlotUtil.getAllSlots( appointmentForm );
            SolrIndexerService.write( FormUtil.getFormItem( appointmentForm, listAllSlots ), sbLogs );
            List<SolrItem> listItems = new ArrayList<>( );
            for ( Slot appointmentSlot : listAllSlots )
            {
                listItems.add( SlotUtil.getSlotItem( appointmentForm, appointmentSlot, listAllSlots ) );
            }
            SolrIndexerService.write( listItems, sbLogs );
        }
    }

    /**
     * Write / Update the slot and then the related form (for the number of available places) to Solr
     * 
     * @param nIdSlot
     *            The id of the slot to write / update
     * @throws IOException
     */
    public void writeSlotAndForm( Slot slot ) throws IOException
    {
        writeSlotAndForm( slot, SolrIndexerService.getSbLogs( ) );
    }

    /**
     * Write / Update the slot and then the related form (for the number of available places) to Solr
     * 
     * @param nIdSlot
     *            The id of the slot to write / update
     * @throws IOException
     */
    public void writeSlotAndForm( Slot slot, StringBuilder sbLogs ) throws IOException
    {
        writeSlotAndForm( slot, sbLogs, null );
    }

    /**
     * Write / update the slot and the related form (for the number of available places) in solr
     * 
     * @param nIdSlot
     *            The id of the slot
     * @param sbLogs
     *            the logs
     * @throws IOException
     */
    public void writeSlotAndForm( Slot slot, StringBuilder sbLogs, Queue<Slot> listSlotToIndex ) throws IOException
    {
        Object lock = getLock( SlotUtil.getSlotUid( slot ) );
        synchronized( lock )
        {
            Set<Slot> listSlotAdded = new HashSet<>( );
            Set<SolrItem> listItems = new HashSet<>( );
            AppointmentFormDTO appointmentForm = FormService.buildAppointmentFormWithoutReservationRule( slot.getIdForm( ) );
            if ( appointmentForm.getIsActive( ) )
            {

                List<Slot> listAllSlots = SlotUtil.getAllSlots( appointmentForm );
                if ( listAllSlots.stream( ).anyMatch( p -> p.getStartingDateTime( ).equals( slot.getStartingDateTime( ) ) ) )
                {
                    listItems.add( SlotUtil.getSlotItem( appointmentForm, slot, listAllSlots ) );
                }
                if ( listSlotToIndex != null )
                {

                    while ( !listSlotToIndex.isEmpty( ) )
                    {

                        Slot slt = listSlotToIndex.poll( );
                        if ( listAllSlots.stream( ).anyMatch( p -> p.getStartingDateTime( ).equals( slt.getStartingDateTime( ) ) ) )
                        {
                            SolrItem item = SlotUtil.getSlotItem( appointmentForm, slt, listAllSlots );
                            listItems.removeIf( p -> p.getUid( ).equals( item.getUid( ) ) );
                            listItems.add( item );
                            listAllSlots.removeIf( p -> p.getStartingDateTime( ).isEqual( slt.getStartingDateTime( ) ) );
                            listAllSlots.add( slt );
                            listSlotAdded.add( slt );
                        }
                    }
                }

                for ( Slot otherSlot : listAllSlots )
                {
                    if ( ( otherSlot.getDate( ).equals( slot.getDate( ) ) && otherSlot.getStartingDateTime( ).isBefore( slot.getStartingDateTime( ) ) )
                            || listSlotAdded.stream( ).anyMatch( slt -> slt.getDate( ).equals( otherSlot.getDate( ) )
                                    && otherSlot.getStartingDateTime( ).isBefore( slt.getStartingDateTime( ) ) ) )
                    {
                        SolrItem item = SlotUtil.getSlotItem( appointmentForm, otherSlot, listAllSlots );
                        listItems.removeIf( p -> p.getUid( ).equals( item.getUid( ) ) );
                        listItems.add( item );
                    }
                }
                if ( !listItems.isEmpty( ) )
                {
                    SolrIndexerService.write( FormUtil.getFormItem( appointmentForm, listAllSlots ), sbLogs );
                    SolrIndexerService.write( listItems, sbLogs );
                }
            }
        }
    }

    /**
     * Delete the Appointment Form and all the related slots in Solr
     * 
     * @param nIdForm
     *            The id of the Form
     * @param sbLogs
     *            the logs
     * @throws SolrServerException
     * @throws IOException
     */
    public void deleteFormAndListSlots( int nIdForm, StringBuilder sbLogs ) throws SolrServerException, IOException
    {
        Object lock = getLock( Utilities.buildResourceUid( Integer.toString( nIdForm ), Utilities.RESOURCE_TYPE_APPOINTMENT ) );
        synchronized( lock )
        {
            // Remove all indexed values of this site
            StringBuffer sbAppointmentFormUidEscaped = new StringBuffer( ClientUtils.escapeQueryChars( SolrIndexerService.getWebAppName( ) ) );
            sbAppointmentFormUidEscaped.append( Utilities.UNDERSCORE )
                    .append( getResourceUid( Integer.toString( nIdForm ), Utilities.RESOURCE_TYPE_APPOINTMENT ) );
            StringBuffer sbQuery = new StringBuffer( SearchItem.FIELD_UID ).append( ":" ).append( sbAppointmentFormUidEscaped ).append( " OR uid_form_string:" )
                    .append( sbAppointmentFormUidEscaped );
            sbLogs.append( "Delete by query: " ).append( sbQuery ).append( StringUtils.CR ).append( StringUtils.LF );
            UpdateResponse update = SolrServerService.getInstance( ).getSolrServer( ).deleteByQuery( sbQuery.toString( ), 1000 );
            sbLogs.append( "Server response: " ).append( update ).append( StringUtils.CR ).append( StringUtils.LF );
        }
    }

    /**
     * Delete the slot in solr
     * 
     * @param slot
     *            The slot to delete
     * @param sbLogs
     *            the logs
     * @throws SolrServerException
     * @throws IOException
     */
    public void deleteSlot( Slot slot, StringBuilder sbLogs ) throws SolrServerException, IOException
    {
        Object lock = getLock( SlotUtil.getSlotUid( slot ) );
        synchronized( lock )
        {
            StringBuffer sbSlotUidEscaped = new StringBuffer( ClientUtils.escapeQueryChars( SolrIndexerService.getWebAppName( ) ) )
                    .append( Utilities.UNDERSCORE ).append( getResourceUid( SlotUtil.getSlotUid( slot ), Utilities.RESOURCE_TYPE_SLOT ) );
            StringBuffer sbQuery = new StringBuffer( SearchItem.FIELD_UID ).append( ":" ).append( sbSlotUidEscaped );
            sbLogs.append( "Delete by query: " ).append( sbQuery ).append( StringUtils.CR ).append( StringUtils.LF );
            UpdateResponse update = SolrServerService.getInstance( ).getSolrServer( ).deleteByQuery( sbQuery.toString( ), 1000 );
            sbLogs.append( "Server response: " ).append( update ).append( StringUtils.CR ).append( StringUtils.LF );
        }
    }

    private static synchronized Object getLock( String key )
    {
        _lockIndexer.putIfAbsent( key, new Object( ) );
        return _lockIndexer.get( key );
    }
}
