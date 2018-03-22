package fr.paris.lutece.plugins.appointment.modules.solr.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.appointment.business.AppointmentForm;
import fr.paris.lutece.plugins.appointment.business.category.Category;
import fr.paris.lutece.plugins.appointment.business.category.CategoryHome;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.util.url.UrlItem;

/**
 * Utils for the Appointment Form (Item, Url, Uid ...)
 * @author Laurent Payen
 *
 */
public class FormUtil {

	private static final String MIN_HOURS_BEFORE_APPOINTMENT = "min_hours_before_appointment";
	private static final String APPOINTMENT_ACTIVE = "appointment_active";
	private static final String URL_BASE = "url_base";
	private static final String FORM_ID_TITLE = "form_id_title";
	private static final String APPOINTMENT_NB_FREE_PLACES = "appointment_nb_free_places";
	private static final String APPOINTMENT_NB_PLACES = "appointment_nb_places";
	private static final String VIEW_APPOINTMENT = "getViewAppointmentCalendar";
	
	private static final String FORM_ID_TITLE_SEPARATOR = "|";
	private static final String DASH = "-";
	private static final String SLASH = "/";
	
	public static final String PARAMETER_ID_FORM = "id_form";
	
	/**
	 * Get the form Uid
	 * 
	 * @param nIdForm
	 *            the form id
	 * @return the form Uid
	 */
	public static String getFormUid(int nIdForm) {
		return SolrIndexerService.getWebAppName() + Utilities.UNDERSCORE
				+ Utilities.buildResourceUid(Integer.toString(nIdForm), Utilities.RESOURCE_TYPE_APPOINTMENT);
	}

	/**
	 * Get the form url
	 * 
	 * @param nIdForm
	 *            the form id
	 * @return the form url
	 */
	public static String getFormUrl(int nIdForm) {
		UrlItem url = new UrlItem(SolrIndexerService.getBaseUrl());
		url.addParameter(Utilities.PARAMETER_XPAGE, Utilities.XPAGE_APPOINTMENT);
		url.addParameter(Utilities.PARAMETER_VIEW, VIEW_APPOINTMENT);
		url.addParameter(PARAMETER_ID_FORM, nIdForm);
		return url.getUrl();
	}

	/**
	 * Build and return the default form item for Solr
	 * 
	 * @param appointmentForm
	 *            the appointment form
	 * @return the form item
	 * @throws IOException
	 */
	public static SolrItem getDefaultFormItem(AppointmentForm appointmentForm) throws IOException {
		SolrItem item = new SolrItem();
		item.setSummary(appointmentForm.getDescription());
		item.setTitle(appointmentForm.getTitle());
		item.setSite(SolrIndexerService.getWebAppName());
		item.setRole("none");
		item.setXmlContent(StringUtils.EMPTY);
		Category category = CategoryHome.findByPrimaryKey(appointmentForm.getIdCategory());
		if (category != null) {
			item.setCategorie(Arrays.asList(category.getLabel()));
		}
		StringBuilder sb = new StringBuilder();
		item.setContent(sb.toString());
		item.addDynamicField(MIN_HOURS_BEFORE_APPOINTMENT, (long) appointmentForm.getMinTimeBeforeAppointment());
		item.addDynamicFieldNotAnalysed(APPOINTMENT_ACTIVE, Boolean.toString(appointmentForm.getIsActive()));
		item.addDynamicFieldNotAnalysed(URL_BASE, SolrIndexerService.getRootUrl());
		item.addDynamicFieldNotAnalysed(FORM_ID_TITLE, getFormUid(appointmentForm.getIdForm())
				+ FORM_ID_TITLE_SEPARATOR + appointmentForm.getTitle());
		return item;
	}

	/**
	 * Build and return the Form Item for Solr
	 * 
	 * @param appointmentForm
	 *            the Appointment Form
	 * @param listSlots
	 *            the list of the slots of the form
	 * @return the Form Item
	 * @throws IOException
	 */
	public static SolrItem getFormItem(AppointmentForm appointmentForm, List<Slot> listSlots) throws IOException {
		SolrItem item = getDefaultFormItem(appointmentForm);
		item.setUrl(getFormUrl(appointmentForm.getIdForm()));
		item.setUid(Utilities.buildResourceUid(Integer.toString(appointmentForm.getIdForm()), Utilities.RESOURCE_TYPE_APPOINTMENT));
		item.setDate(appointmentForm.getDateStartValidity());
		item.setType(Utilities.SHORT_NAME_APPOINTMENT);
		int free_places = 0;
		int places = 0;
		for (Slot slot : listSlots) {
			free_places += slot.getNbPotentialRemainingPlaces();
			places += slot.getMaxCapacity();
		}
		if (StringUtils.isNotEmpty(appointmentForm.getAddress()) && appointmentForm.getLongitude() != null
				&& appointmentForm.getLatitude() != null) {
			item.addDynamicFieldGeoloc(Utilities.SHORT_NAME_APPOINTMENT, appointmentForm.getAddress(), appointmentForm.getLongitude(),
					appointmentForm.getLatitude(), Utilities.SHORT_NAME_APPOINTMENT + DASH + free_places + SLASH + places);
		}
		item.addDynamicField(APPOINTMENT_NB_FREE_PLACES, Long.valueOf(free_places));
		item.addDynamicField(APPOINTMENT_NB_PLACES, Long.valueOf(places));
		// Date Hierarchy
		if (appointmentForm.getDateStartValidity() != null) {
			item.setHieDate(appointmentForm.getDateStartValidity().toLocalDate().format(Utilities.HIE_DATE_FORMATTER));
		}
		return item;
	}

}
