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
