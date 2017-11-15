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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;

import fr.paris.lutece.plugins.appointment.business.AppointmentForm;
import fr.paris.lutece.plugins.appointment.business.category.Category;
import fr.paris.lutece.plugins.appointment.business.category.CategoryHome;
import fr.paris.lutece.plugins.appointment.business.display.Display;
import fr.paris.lutece.plugins.appointment.business.form.Form;
import fr.paris.lutece.plugins.appointment.business.form.FormHome;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.business.slot.SlotHome;
import fr.paris.lutece.plugins.appointment.service.DisplayService;
import fr.paris.lutece.plugins.appointment.service.FormRuleService;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.WeekDefinitionService;
import fr.paris.lutece.plugins.search.solr.business.SolrServerService;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.portal.service.search.SearchItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.url.UrlItem;

public class SolrAppointmentIndexer implements SolrIndexer
{

    public static final String BEAN_NAME = "appointment-solr.solrIdeeIndexer";
    public static final String RESOURCE_TYPE_APPOINTMENT = "appointment";
    public static final String RESOURCE_TYPE_SLOT = "slot";

    private static final String SHORT_NAME_APPOINTMENT = "appointment";
    private static final String SHORT_NAME_SLOT = "appointment-slot";
    private static final String PARAMETER_XPAGE = "page";
    private static final String XPAGE_APPOINTMENT = "appointment";
    private static final String PARAMETER_ID_FORM = "id_form";
    private static final String PARAMETER_ID_SLOT = "idSlot";
    private static final String PARAMETER_VIEW = "view";
    private static final String VIEW_APPOINTMENT = "getAppointmentFormFirstStep";
    private static final String PARAMETER_ACTION = "action";
    private static final String ACTION_SELECT_SLOT = "doSelectSlot";

    public static final String FORM_ID_TITLE_SEPARATOR = "|";
    
    private SolrItem getDefaultItem( AppointmentForm appointmentForm )
        throws IOException
    {
        SolrItem item = new SolrItem(  );
        item.setSummary( appointmentForm.getDescription() );
        item.setTitle( appointmentForm.getTitle() );
        item.setSite( SolrIndexerService.getWebAppName(  ) );
        item.setRole( "none" );
        item.setXmlContent("");
        Category category = CategoryHome.findByPrimaryKey( appointmentForm.getIdCategory() );
        if ( category != null ) {
            item.setCategorie(Arrays.asList( category.getLabel() ));
        }
        StringBuilder sb = new StringBuilder();
        item.setContent( sb.toString() );

        int minTimeBeforeAppointment =  FormRuleService.findFormRuleWithFormId( appointmentForm.getIdForm() ).getMinTimeBeforeAppointment( );
        item.addDynamicField( "min_hours_before_appointment", (long) minTimeBeforeAppointment);
        item.addDynamicFieldNotAnalysed( "appointment_active", Boolean.toString( appointmentForm.getIsActive( ) ) );
        item.addDynamicFieldNotAnalysed( "url_base", SolrIndexerService.getRootUrl( ) );
        item.addDynamicFieldNotAnalysed( "form_id_title", getFormUid(appointmentForm.getIdForm()) + FORM_ID_TITLE_SEPARATOR + appointmentForm.getTitle() );
        return item;
    }

    private String getFormUrl( int nIdForm ) {
        UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
        url.addParameter( PARAMETER_XPAGE, XPAGE_APPOINTMENT );
        url.addParameter( PARAMETER_VIEW, VIEW_APPOINTMENT );
        url.addParameter( PARAMETER_ID_FORM, nIdForm );
        return url.getUrl();
    }

    private String getFormUid( int nIdForm ) {
        return SolrIndexerService.getWebAppName() + "_" + getResourceUid( Integer.toString(nIdForm), RESOURCE_TYPE_APPOINTMENT );
    }

    private SolrItem getItem( AppointmentForm appointmentForm, List<Slot> listSlots )
        throws IOException
    {
        SolrItem item = getDefaultItem( appointmentForm );

        item.setUrl( getFormUrl( appointmentForm.getIdForm(  ) ) );

        item.setUid( getResourceUid( Integer.toString(appointmentForm.getIdForm()),
                RESOURCE_TYPE_APPOINTMENT ) );
        item.setDate( appointmentForm.getDateStartValidity() );
        item.setType( SHORT_NAME_APPOINTMENT );

        int free_places = 0;
        int places = 0;
        for (Slot slot : listSlots) {
            free_places += slot.getNbPotentialRemainingPlaces();
            places += slot.getMaxCapacity();
        }
        if (appointmentForm.getAddress() != null &&
            appointmentForm.getLongitude( ) != null &&
            appointmentForm.getLatitude( ) != null) {

            item.addDynamicFieldGeoloc("appointment", appointmentForm.getAddress(),
                   appointmentForm.getLongitude( ),
                   appointmentForm.getLatitude( ),
            "appointment-" + free_places + "/" + places);
        }
        item.addDynamicField( "appointment_nb_free_places", (long) free_places);
        item.addDynamicField( "appointment_nb_places", (long) places);

        // Date Hierarchy
        if ( appointmentForm.getDateStartValidity() != null ) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime( appointmentForm.getDateStartValidity() );
            item.setHieDate( calendar.get( GregorianCalendar.YEAR ) + "/" + ( calendar.get( GregorianCalendar.MONTH ) + 1 ) + "/" +
                    calendar.get( GregorianCalendar.DAY_OF_MONTH ) + "/" );
        }


        return item;
    }

    private static final DateTimeFormatter SlotSolrIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    /**
     * Generate a unique ID for solr.
     *
     * Slots don't have ids anymore, so we use the form_id and the slot date as an ID.
     * We try to make a "readable" id with the form id and the slot datetime, using
     * only alphanumerical caracters to avoid potential problems with code parsing this ID.
     * 
     */
    private String getSolrSlotId( Slot appointmentSlot ) {

        String strSlotDateFormatted = appointmentSlot.getStartingDateTime().format(SlotSolrIdFormatter);

        return "F" + appointmentSlot.getIdForm() + "D" + strSlotDateFormatted ;
    }

    private SolrItem getItem( AppointmentForm appointmentForm, Slot appointmentSlot )
        throws IOException
    {
        // the item
        SolrItem item = getDefaultItem(appointmentForm);
        item.setUid( getResourceUid( getSolrSlotId ( appointmentSlot ) , RESOURCE_TYPE_SLOT ) );
        item.addDynamicFieldNotAnalysed( "uid_form", getFormUid( appointmentForm.getIdForm() ) );
        UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
        url.addParameter( PARAMETER_XPAGE, XPAGE_APPOINTMENT );
        url.addParameter( PARAMETER_ACTION, ACTION_SELECT_SLOT );
        url.addParameter( PARAMETER_ID_SLOT, appointmentSlot.getIdSlot() );
        item.setUrl( url.getUrl(  ) );
        item.addDynamicFieldNotAnalysed( "url_form", getFormUrl( appointmentForm.getIdForm(  ) ) );

        
        //TODO correctly handle the timezone/offset instead of hardcoding UTC
        Calendar cal = GregorianCalendar.from( appointmentSlot.getStartingDateTime().atZone( ZoneOffset.UTC ) );
        item.setDate( cal.getTime() );
        item.setDate( appointmentSlot.getStartingTimestampDate() );
        
        item.setType( SHORT_NAME_SLOT );

        if (appointmentForm.getAddress() != null &&
            appointmentForm.getLongitude( ) != null &&
            appointmentForm.getLatitude( ) != null) {

            item.addDynamicFieldGeoloc("appointmentslot", appointmentForm.getAddress(),
                   appointmentForm.getLongitude( ),
                   appointmentForm.getLatitude( ),
            "appointmentslot-" + appointmentSlot.getNbPotentialRemainingPlaces() + "/" + appointmentSlot.getMaxCapacity());
        }

        item.addDynamicFieldNotAnalysed( "day_open", Boolean.toString( true ) );
        item.addDynamicFieldNotAnalysed( "enabled", Boolean.toString( appointmentSlot.getIsOpen() ) );
        item.addDynamicField( "slot_nb_free_places", (long) appointmentSlot.getNbPotentialRemainingPlaces());
        item.addDynamicField( "slot_nb_places", (long) appointmentSlot.getMaxCapacity());

        item.addDynamicField( "day_of_week", (long) appointmentSlot.getDate().getDayOfWeek().getValue() );
        item.addDynamicField( "minute_of_day", (long) appointmentSlot.getStartingDateTime().getHour()*60+appointmentSlot.getStartingDateTime().getMinute());

        // Date Hierarchy
        item.setHieDate( cal.get( GregorianCalendar.YEAR ) + "/" + ( cal.get( GregorianCalendar.MONTH ) + 1 ) + "/" +
                cal.get( GregorianCalendar.DAY_OF_MONTH ) + "/" );
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceUid( String strResourceId, String strResourceType )
    {
        StringBuilder sb = new StringBuilder( strResourceId );
        if (RESOURCE_TYPE_SLOT.equals(strResourceType)) {
            sb.append( '_' ).append( SHORT_NAME_SLOT );
        } else if (RESOURCE_TYPE_APPOINTMENT.equals(strResourceType)) {
            sb.append( '_' ).append( SHORT_NAME_APPOINTMENT );
        }  else {
            AppLogService.error("SolrAppointmentIndexer, unknown resourceType: " + strResourceType);
            return null;
        }

        return sb.toString(  );
    }

    @Override
    public List<Field> getAdditionalFields() {
        // TODO Auto-generated method stub
        return new ArrayList();
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return "Appointments and slots indexer";
    }

    @Override
    public List<SolrItem> getDocuments(String arg0) {
        // TODO Auto-generated method stub
        return new ArrayList();
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "appointmentForm";
    }

    @Override
    public List<String> getResourcesName() {
        // TODO Auto-generated method stub
        return new ArrayList();
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return "1.0.0";
    }

    @Override
    public synchronized List<String> indexDocuments() {
        List<String> errors = new ArrayList<String>();
     
        //TODO don't load forms twice
        for (Form form: FormHome.findAllForms())
        {
            AppointmentForm appointmentForm = FormService.buildAppointmentForm( form.getIdForm(), 0, 0 );
            try
            {
                writeAppointmentFormAndSlots( appointmentForm );
            } catch ( Exception e )
            {
                AppLogService.error( "Error indexing AppointmentForm" + appointmentForm.getIdForm(), e);
                errors.add(e.toString());
            }
        }
        return errors;
    }

    public synchronized void writeAppointmentFormAndSlots(AppointmentForm appointmentForm)
            throws CorruptIndexException, IOException
    {
        writeAppointmentFormAndSlots(appointmentForm, SolrIndexerService.getSbLogs(  ) );
    }

    public synchronized void writeAppointmentFormAndSlots(AppointmentForm appointmentForm, StringBuffer sbLogs )
            throws CorruptIndexException, IOException {

        List<Slot> listAllSlots = getAllSlots(appointmentForm);

        SolrIndexerService.write(getItem(appointmentForm, listAllSlots), sbLogs);
        List<SolrItem> listItems = new ArrayList<>();
        for (Slot appointmentSlot : listAllSlots) {
            listItems.add(getItem(appointmentForm, appointmentSlot));
        }
        SolrIndexerService.write(listItems, sbLogs);
    }

    private List<Slot> getAllSlots(AppointmentForm appointmentForm) {
        HashMap<LocalDate, WeekDefinition> mapWeekDefinition = WeekDefinitionService.findAllWeekDefinition( appointmentForm.getIdForm() );
        Display display = DisplayService.findDisplayWithFormId( appointmentForm.getIdForm() );
        // Get the nb weeks to display
        int nNbWeeksToDisplay = display.getNbWeeksToDisplay( );

        LocalDate startingDateOfDisplay = LocalDate.now( );
        LocalDate endingDateOfDisplay = startingDateOfDisplay.plusWeeks( nNbWeeksToDisplay );
        LocalDate endingValidityDate = appointmentForm.getDateEndValidity ( ).toLocalDate();
        if ( endingValidityDate != null )
        {
            if ( endingDateOfDisplay.isAfter( endingValidityDate ) )
            {
                nNbWeeksToDisplay = Math.toIntExact( startingDateOfDisplay.until( endingDateOfDisplay, ChronoUnit.WEEKS ) );
            }
        }

        return SlotService.buildListSlot( appointmentForm.getIdForm(), mapWeekDefinition, startingDateOfDisplay, nNbWeeksToDisplay );
    }

    public synchronized void deleteAppointmentFormAndSlots(int nIdForm, StringBuffer sbLogs)
            throws SolrServerException, IOException {
        // Remove all indexed values of this site
        String strAppointmentFormUidEscaped =
            ClientUtils.escapeQueryChars ( SolrIndexerService.getWebAppName( ) ) + "_" + getResourceUid( Integer.toString(nIdForm), RESOURCE_TYPE_APPOINTMENT );
        String query =
            SearchItem.FIELD_UID + ":" + strAppointmentFormUidEscaped + " OR " +
            "uid_form_string" + ":" + strAppointmentFormUidEscaped ;
        sbLogs.append ("Delete by query: " + query + "\r\n" );
        UpdateResponse update = SolrServerService.getInstance(  ).getSolrServer(  ).deleteByQuery( query, 1000 );
        sbLogs.append("Server response: " + update + "\r\n" );
    }

    public synchronized void writeAppointmentSlot(int nIdSlot, StringBuffer sbLogs)
            throws CorruptIndexException, IOException
    {
        Slot appointmentSlot = SlotHome.findByPrimaryKey( nIdSlot );
        AppointmentForm appointmentForm = FormService.buildAppointmentForm( appointmentSlot.getIdForm(), 0 , 0);
        SolrIndexerService.write(getItem(appointmentForm, appointmentSlot), sbLogs);
    }

    public synchronized void deleteSlot(Slot appointmentSlot, StringBuffer sbLogs)
            throws SolrServerException, IOException {
        String strSlotUidEscaped =
            ClientUtils.escapeQueryChars ( SolrIndexerService.getWebAppName( ) ) + "_" + getResourceUid( getSolrSlotId ( appointmentSlot ) , RESOURCE_TYPE_SLOT );
        String query = SearchItem.FIELD_UID + ":" + strSlotUidEscaped;
        sbLogs.append ("Delete by query: " + query + "\r\n" );
        UpdateResponse update = SolrServerService.getInstance(  ).getSolrServer(  ).deleteByQuery( query, 1000 );
        sbLogs.append("Server response: " + update + "\r\n" );
    }

    @Override
    public boolean isEnable() {
        // TODO Auto-generated method stub
        return true;
    }
    
}
