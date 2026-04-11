package com.example.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MemberTest {

    // ---- isCardValid ----

    @Test
    void isCardValid_activeAndNotExpired_returnsTrue() {
        Member m = new Member();
        m.setStatus(Member.Status.ACTIVE);
        m.setExpiryDate(LocalDate.now().plusDays(1));
        assertTrue(m.isCardValid());
    }

    @Test
    void isCardValid_activeButExpiredYesterday_returnsFalse() {
        Member m = new Member();
        m.setStatus(Member.Status.ACTIVE);
        m.setExpiryDate(LocalDate.now().minusDays(1));
        assertFalse(m.isCardValid());
    }

    @Test
    void isCardValid_activeExpiryToday_returnsTrue() {
        // Boundary: expiry == today → !isAfter(today,today) == true (card is still valid today)
        Member m = new Member();
        m.setStatus(Member.Status.ACTIVE);
        m.setExpiryDate(LocalDate.now());
        assertTrue(m.isCardValid());
    }

    @Test
    void isCardValid_suspendedMember_returnsFalse() {
        Member m = new Member();
        m.setStatus(Member.Status.SUSPENDED);
        m.setExpiryDate(LocalDate.now().plusYears(1));
        assertFalse(m.isCardValid());
    }

    @Test
    void isCardValid_expiredStatus_returnsFalse() {
        Member m = new Member();
        m.setStatus(Member.Status.EXPIRED);
        m.setExpiryDate(LocalDate.now().plusDays(5));
        assertFalse(m.isCardValid());
    }

    @Test
    void isCardValid_nullExpiryDate_returnsFalse() {
        Member m = new Member();
        m.setStatus(Member.Status.ACTIVE);
        m.setExpiryDate(null);
        assertFalse(m.isCardValid());
    }
}
