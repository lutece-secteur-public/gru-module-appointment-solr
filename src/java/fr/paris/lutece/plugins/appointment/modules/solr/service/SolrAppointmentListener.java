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
import fr.paris.lutece.plugins.appointment.business.AppointmentFormHome;
import fr.paris.lutece.plugins.appointment.service.listeners.IAppointmentCreationListener;
import fr.paris.lutece.plugins.appointment.service.listeners.IAppointmentFormListener;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;

public class SolrAppointmentListener implements IAppointmentCreationListener, IAppointmentFormListener {

    @Override
    public void onFormModifed(final int nIdForm) {
        (new Thread() {
            @Override
            public void run() {
                StringBuffer sbLogs = new StringBuffer();
                try {
                    AppointmentForm appointmentForm = AppointmentFormHome.findByPrimaryKey( nIdForm );
                    SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( "appointment-solr.solrIdeeIndexer");
                    solrAppointmentIndexer.deleteAppointmentFormAndSlots(nIdForm, sbLogs);
                    if (appointmentForm != null) {
                        solrAppointmentIndexer.writeAppointmentFormAndSlots(appointmentForm, sbLogs);
                    }
                } catch (Exception e) {
                    AppLogService.error ( "Error during SolrAppointmentListener onForModified: " + sbLogs, e);
                }
            }
        }).start();
    }

    @Override
    public void onAppointmentCreated(int nIdSlot) {
        StringBuffer sbLogs = new StringBuffer();
        SolrAppointmentIndexer solrAppointmentIndexer = SpringContextService.getBean( "appointment-solr.solrIdeeIndexer");
        try {
            solrAppointmentIndexer.writeAppointmentSlot(nIdSlot, sbLogs);
        } catch (IOException e) {
            AppLogService.error ( "Error during SolrAppointmentListener onForModified: " + sbLogs, e);
        }
    }
}
