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
