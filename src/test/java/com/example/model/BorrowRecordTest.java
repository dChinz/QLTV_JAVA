package com.example.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BorrowRecordTest {

    // ---- getOverdueDays ----

    @Test
    void getOverdueDays_returnedOnTime_returnsZero() {
        BorrowRecord record = new BorrowRecord(1, 1, LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(9));
        record.setReturnDate(LocalDate.now());
        assertEquals(0, record.getOverdueDays());
    }

    @Test
    void getOverdueDays_returnedLate_returnsCorrectDays() {
        LocalDate due = LocalDate.now().minusDays(3);
        BorrowRecord record = new BorrowRecord(1, 1, due.minusDays(14), due);
        record.setReturnDate(LocalDate.now());
        assertEquals(3, record.getOverdueDays());
    }

    @Test
    void getOverdueDays_notYetReturned_andPastDue_returnsPositiveDays() {
        LocalDate due = LocalDate.now().minusDays(5);
        BorrowRecord record = new BorrowRecord(1, 1, due.minusDays(14), due);
        // returnDate is null → uses today
        assertTrue(record.getOverdueDays() >= 5);
    }

    @Test
    void getOverdueDays_notYetDue_returnsZero() {
        BorrowRecord record = new BorrowRecord(1, 1, LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(11));
        assertEquals(0, record.getOverdueDays());
    }

    @Test
    void getOverdueDays_dueTodayNotReturned_returnsZero() {
        BorrowRecord record = new BorrowRecord(1, 1, LocalDate.now().minusDays(14),
                LocalDate.now());
        assertEquals(0, record.getOverdueDays());
    }

    // ---- isOverdue ----

    @Test
    void isOverdue_returnedLateButStatusReturned_returnsFalse() {
        LocalDate due = LocalDate.now().minusDays(2);
        BorrowRecord record = new BorrowRecord(1, 1, due.minusDays(14), due);
        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowRecord.Status.RETURNED);
        assertFalse(record.isOverdue());
    }

    @Test
    void isOverdue_pastDueAndBorrowing_returnsTrue() {
        LocalDate due = LocalDate.now().minusDays(3);
        BorrowRecord record = new BorrowRecord(1, 1, due.minusDays(14), due);
        // status = BORROWING by default
        assertTrue(record.isOverdue());
    }

    @Test
    void isOverdue_notPastDue_returnsFalse() {
        BorrowRecord record = new BorrowRecord(1, 1, LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(9));
        assertFalse(record.isOverdue());
    }
}
