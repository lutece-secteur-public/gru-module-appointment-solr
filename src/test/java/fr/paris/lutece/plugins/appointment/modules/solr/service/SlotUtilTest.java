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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.test.LuteceTestCase;

public class SlotUtilTest extends LuteceTestCase
{
    public void testCalculateConsecutiveSlots( )
    {
        LocalDateTime l6am = LocalDateTime.now( ).withHour( 6 ).withMinute( 0 ).withSecond( 0 );
        LocalDateTime l7am = LocalDateTime.now( ).withHour( 7 ).withMinute( 0 ).withSecond( 0 );
        LocalDateTime l8am = LocalDateTime.now( ).withHour( 8 ).withMinute( 0 ).withSecond( 0 );
        LocalDateTime l9am = LocalDateTime.now( ).withHour( 9 ).withMinute( 0 ).withSecond( 0 );
        LocalDateTime l10am = LocalDateTime.now( ).withHour( 10 ).withMinute( 0 ).withSecond( 0 );
        LocalDateTime l11am = LocalDateTime.now( ).withHour( 11 ).withMinute( 0 ).withSecond( 0 );

        Slot slot1 = new Slot( );
        slot1.setStartingDateTime( l6am );
        slot1.setEndingDateTime( l7am );
        slot1.setNbPotentialRemainingPlaces( 2 );

        Slot slot2 = new Slot( );
        slot2.setStartingDateTime( l7am );
        slot2.setEndingDateTime( l8am );
        slot2.setNbPotentialRemainingPlaces( 1 );

        Slot slot3 = new Slot( );
        slot3.setStartingDateTime( l8am );
        slot3.setEndingDateTime( l9am );
        slot3.setNbPotentialRemainingPlaces( 0 );

        Slot slot4 = new Slot( );
        slot4.setStartingDateTime( l10am );
        slot4.setEndingDateTime( l11am );
        slot4.setNbPotentialRemainingPlaces( 1 );

        List<Slot> allSlots = new ArrayList<>( );
        allSlots.add( slot1 );
        allSlots.add( slot2 );
        allSlots.add( slot3 );
        allSlots.add( slot4 );

        assertEquals( 2, SlotUtil.calculateConsecutiveSlots( slot1, allSlots ) );
        assertEquals( 1, SlotUtil.calculateConsecutiveSlots( slot2, allSlots ) );
        assertEquals( 0, SlotUtil.calculateConsecutiveSlots( slot3, allSlots ) );
        assertEquals( 1, SlotUtil.calculateConsecutiveSlots( slot4, allSlots ) );
    }
}
