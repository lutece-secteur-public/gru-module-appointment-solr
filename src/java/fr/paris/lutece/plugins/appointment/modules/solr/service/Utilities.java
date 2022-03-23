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

import java.time.format.DateTimeFormatter;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Utilities and constants for Appointment Solr
 * 
 * @author Laurent Payen
 *
 */
public final class Utilities
{

    public static final String APPOINTMENT_FORM_NAME = "appointmentForm";
    public static final String APPOINTMENT_DESCRIPTION = "Appointments and slots indexer";
    public static final String APPOINTMENT_VERSION = "1.0.0";
    public static final String RESOURCE_TYPE_APPOINTMENT = "appointment";
    public static final String RESOURCE_TYPE_SLOT = "slot";
    public static final String SHORT_NAME_APPOINTMENT = "appointment";
    public static final String SHORT_NAME_SLOT = "appointment-slot";
    public static final String PROPERTY_INDEXER_ENABLE = "appointment-solr.indexer.enable";

    public static final String PARAMETER_XPAGE = "page";
    public static final String XPAGE_APPOINTMENT = "appointment";
    public static final String PARAMETER_VIEW = "view";

    public static final String UNDERSCORE = "_";

    public static final DateTimeFormatter SLOT_SOLR_ID_DATE_FORMATTER = DateTimeFormatter.ofPattern( "yyyyMMdd'T'HHmmss" );
    public static final String FORMAT_HIE_DATE = "yyyy/MM/dd";
    public static final DateTimeFormatter HIE_DATE_FORMATTER = DateTimeFormatter.ofPattern( FORMAT_HIE_DATE );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private Utilities( )
    {
    }

    /**
     * Build the resource unique Id
     * 
     * @param strResourceId
     *            the id of the resource
     * @param strResourceType
     *            the type of the resource
     * @return the unique id
     */
    public static String buildResourceUid( String strResourceId, String strResourceType )
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
}
