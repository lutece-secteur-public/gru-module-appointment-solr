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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.WeekDefinitionService;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import fr.paris.lutece.util.url.UrlItem;

/**
 * Utils for the slots (Uid, Url, Item ...)
 * 
 * @author Laurent Payen
 *
 */
public final class SlotUtil
{

    private static final String DAY_OPEN = "day_open";
    private static final String ENABLED = "enabled";
    private static final String SLOT_NB_FREE_PLACES = "slot_nb_free_places";
    private static final String SLOT_NB_PLACES = "slot_nb_places";
    private static final String DAY_OF_WEEK = "day_of_week";
    private static final String MINUTE_OF_DAY = "minute_of_day";
    private static final String NB_CONSECUTIVES_SLOTS = "nb_consecutives_slots";
    private static final String UID_FORM = "uid_form";
    private static final String URL_FORM = "url_form";
    private static final String APPOINTMENT_SLOT = "appointmentslot";
    private static final String VIEW_FORM = "getViewAppointmentForm";

    private static final String PARAMETER_STARTING_DATETIME = "starting_date_time";
    private static final String PARAMETER_ANCHOR = "anchor";
    private static final String VALUE_ANCHOR = "step3";

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private SlotUtil( )
    {
    }

    /**
     * Generate a unique ID for solr.
     *
     * Slots don't have ids anymore, so we use the form_id and the slot date as an ID. We try to make a "readable" id with the form id and the slot datetime,
     * using only alphanumerical caracters to avoid potential problems with code parsing this ID.
     * 
     */
    public static String getSlotUid( Slot slot )
    {
        String strSlotDateFormatted = slot.getStartingDateTime( ).format( Utilities.SLOT_SOLR_ID_DATE_FORMATTER );
        return "F" + slot.getIdForm( ) + "D" + strSlotDateFormatted;
    }

    /**
     * Get the slot url to call directly rdv v2 with the good parameters
     * 
     * @param slot
     *            the slot
     * @return the url with all the parameters
     */
    public static String getSlotUrl( Slot slot )
    {
        UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl( ) );
        url.addParameter( Utilities.PARAMETER_XPAGE, Utilities.XPAGE_APPOINTMENT );
        url.addParameter( Utilities.PARAMETER_VIEW, VIEW_FORM );
        url.addParameter( FormUtil.PARAMETER_ID_FORM, slot.getIdForm( ) );
        url.addParameter( PARAMETER_STARTING_DATETIME, slot.getStartingDateTime( ).toString( ) );
        url.addParameter( PARAMETER_ANCHOR, VALUE_ANCHOR );
        return url.getUrl( );
    }

    /**
     * Build and return the slot Item for Solr
     * 
     * @param appointmentForm
     *            the Appointment Form
     * @param slot
     *            the slot
     * @return the slot Item
     */
    public static SolrItem getSlotItem( AppointmentFormDTO appointmentForm, Slot slot, List<Slot> allSlots )
    {
        // the item
        SolrItem item = FormUtil.getDefaultFormItem( appointmentForm );
        item.setUid( Utilities.buildResourceUid( getSlotUid( slot ), Utilities.RESOURCE_TYPE_SLOT ) );
        item.addDynamicFieldNotAnalysed( UID_FORM, FormUtil.getFormUid( appointmentForm.getIdForm( ) ) );
        item.setUrl( getSlotUrl( slot ) );
        item.addDynamicFieldNotAnalysed( URL_FORM, FormUtil.getFormUrl( appointmentForm.getIdForm( ) ) );
        item.setDate( slot.getStartingTimestampDate( ) );
        item.setType( Utilities.SHORT_NAME_SLOT );
        if ( StringUtils.isNotEmpty( appointmentForm.getAddress( ) ) && appointmentForm.getLongitude( ) != null && appointmentForm.getLatitude( ) != null )
        {
            item.addDynamicFieldGeoloc( APPOINTMENT_SLOT, appointmentForm.getAddress( ), appointmentForm.getLongitude( ), appointmentForm.getLatitude( ),
                    "appointmentslot-" + slot.getNbPotentialRemainingPlaces( ) + "/" + slot.getMaxCapacity( ) );
        }
        item.addDynamicFieldNotAnalysed( DAY_OPEN, String.valueOf( Boolean.TRUE ) );
        item.addDynamicFieldNotAnalysed( ENABLED, String.valueOf( slot.getIsOpen( ) ) );
        item.addDynamicField( SLOT_NB_FREE_PLACES, Long.valueOf( slot.getNbPotentialRemainingPlaces( ) ) );
        item.addDynamicField( SLOT_NB_PLACES, Long.valueOf( slot.getMaxCapacity( ) ) );
        item.addDynamicField( DAY_OF_WEEK, Long.valueOf( slot.getStartingDateTime( ).getDayOfWeek( ).getValue( ) ) );
        item.addDynamicField( MINUTE_OF_DAY,
                ChronoUnit.MINUTES.between( slot.getStartingDateTime( ).toLocalDate( ).atStartOfDay( ), slot.getStartingDateTime( ) ) );
        item.addDynamicField( NB_CONSECUTIVES_SLOTS, (long) calculateConsecutiveSlots( slot, allSlots ) );

        // Date Hierarchy
        item.setHieDate( slot.getStartingDateTime( ).toLocalDate( ).format( Utilities.HIE_DATE_FORMATTER ) );
        return item;
    }

    /**
     * Get all the slots of a form by calling the method buildListSlot of the plugin RDV
     * 
     * @param appointmentForm
     *            the appointment form
     * @return all the slots of a form
     */
    public static List<Slot> getAllSlots( AppointmentFormDTO appointmentForm )
    {
        // Get the nb weeks to display
        int nNbWeeksToDisplay = appointmentForm.getNbWeeksToDisplay( );
        LocalDate startingDateOfDisplay = LocalDate.now( );
        if ( appointmentForm.getDateStartValidity( ) != null && startingDateOfDisplay.isBefore( appointmentForm.getDateStartValidity( ).toLocalDate( ) ) )
        {
            startingDateOfDisplay = appointmentForm.getDateStartValidity( ).toLocalDate( );
        }
        // Calculate the ending date of display with the nb weeks to display
        // since today
        // We calculate the number of weeks including the current week, so it
        // will end to the (n) next sunday
        TemporalField fieldISO = WeekFields.of( LocaleService.getDefault( ) ).dayOfWeek( );
        LocalDate dateOfSunday = startingDateOfDisplay.with( fieldISO, DayOfWeek.SUNDAY.getValue( ) );
        LocalDate endingDateOfDisplay = dateOfSunday.plusWeeks( nNbWeeksToDisplay - 1L );
        LocalDate endingValidityDate = null;
        if ( appointmentForm.getDateEndValidity( ) != null )
        {
            endingValidityDate = appointmentForm.getDateEndValidity( ).toLocalDate( );
        }
        if ( endingValidityDate != null && endingDateOfDisplay.isAfter( endingValidityDate ) )
        {
            endingDateOfDisplay = endingValidityDate;
        }
        List<Slot> listSlots = SlotService.buildListSlot( appointmentForm.getIdForm( ),
                WeekDefinitionService.findAllWeekDefinition( appointmentForm.getIdForm( ) ), startingDateOfDisplay, endingDateOfDisplay );
        // Get the min time from now before a user can take an appointment (in hours)
        // Filter the list of slots
        if ( CollectionUtils.isNotEmpty( listSlots ) && appointmentForm.getMinTimeBeforeAppointment( ) != 0 )
        {
            LocalDateTime dateTimeBeforeAppointment = LocalDateTime.now( ).plusHours( appointmentForm.getMinTimeBeforeAppointment( ) );
            listSlots = listSlots.stream( ).filter( s -> s.getStartingDateTime( ).isAfter( dateTimeBeforeAppointment ) ).collect( Collectors.toList( ) );
        }

        return listSlots;
    }

    public static int calculateConsecutiveSlots( Slot slot, List<Slot> allSlots )
    {
        if ( slot.getNbPotentialRemainingPlaces( ) <= 0 )
        {
            return 0;
        }
        AtomicInteger consecutiveSlots = new AtomicInteger( 1 );
        doCalculateConsecutiveSlots( slot, allSlots, consecutiveSlots );
        return consecutiveSlots.get( );
    }

    private static void doCalculateConsecutiveSlots( Slot slot, List<Slot> allSlots, AtomicInteger consecutiveSlots )
    {
        for ( Slot nextSlot : allSlots )
        {
            if ( Objects.equals( slot.getEndingDateTime( ), nextSlot.getStartingDateTime( ) ) )
            {
                if ( nextSlot.getNbPotentialRemainingPlaces( ) > 0 && nextSlot.getIsOpen( ) )
                {
                    consecutiveSlots.addAndGet( 1 );
                    doCalculateConsecutiveSlots( nextSlot, allSlots, consecutiveSlots );
                }
                else
                {
                    break;
                }
            }
        }
    }
}
