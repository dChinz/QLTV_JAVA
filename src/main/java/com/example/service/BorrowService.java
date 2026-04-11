package com.example.service;

import com.example.dao.BookDAO;
import com.example.dao.BorrowRecordDAO;
import com.example.dao.FineDAO;
import com.example.dao.MemberDAO;
import com.example.model.Book;
import com.example.model.BorrowRecord;
import com.example.model.Fine;
import com.example.model.Member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BorrowService {

    /** Fine rate: 5,000 VND per overdue day */
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("5000");
    /** Replacement fee for a lost book */
    public static final BigDecimal LOST_BOOK_FEE = new BigDecimal("200000");
    /** Default loan period in days */
    public static final int DEFAULT_LOAN_DAYS = 14;

    private final BorrowRecordDAO borrowDAO;
    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final FineDAO fineDAO;

    public BorrowService() {
        this.borrowDAO = new BorrowRecordDAO();
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
        this.fineDAO = new FineDAO();
    }

    /** Package-private constructor for testing. */
    BorrowService(BorrowRecordDAO borrowDAO, BookDAO bookDAO, MemberDAO memberDAO, FineDAO fineDAO) {
        this.borrowDAO = borrowDAO;
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.fineDAO = fineDAO;
    }

    public List<BorrowRecord> getAllRecords() {
        return borrowDAO.findAll();
    }

    public List<BorrowRecord> getActiveBorrows() {
        return borrowDAO.findActiveBorrows();
    }

    public List<BorrowRecord> getOverdueRecords() {
        return borrowDAO.findOverdue();
    }

    public List<BorrowRecord> getBorrowsByMember(int memberId) {
        return borrowDAO.findByMember(memberId);
    }

    public List<BorrowRecord> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return borrowDAO.findAll();
        return borrowDAO.search(keyword.trim());
    }

    /**
     * Processes a new borrow request.
     * Validates member card validity, book availability, and no existing unreturned borrow for same book.
     */
    public BorrowRecord borrowBook(int memberId, int bookId, int loanDays) {
        Member member = memberDAO.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Đọc giả không tìm thấy."));
        if (!member.isCardValid())
            throw new IllegalStateException("Thẻ đọc giả hết hạn hoặc bị khóa. Vui lòng gia hạn thẻ.");

        Book book = bookDAO.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Sách không tìm thấy."));
        if (!book.isAvailable())
            throw new IllegalStateException("Sách '" + book.getTitle() + "' hiện không có sẵn.");

        // Check member does not already have this book borrowed
        List<BorrowRecord> existing = borrowDAO.findByMember(memberId);
        boolean alreadyBorrowed = existing.stream().anyMatch(r ->
            r.getBookId() == bookId && r.getStatus() == BorrowRecord.Status.BORROWING);
        if (alreadyBorrowed)
            throw new IllegalStateException("Đọc giả đang mượn cuốn sách này rồi.");

        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(memberId, bookId, today, today.plusDays(loanDays));
        borrowDAO.save(record);
        bookDAO.updateAvailableCopies(bookId, -1);
        return record;
    }

    public BorrowRecord borrowBook(int memberId, int bookId) {
        return borrowBook(memberId, bookId, DEFAULT_LOAN_DAYS);
    }

    /**
     * Processes a book return.
     * Calculates and creates a fine if returned late.
     */
    public Optional<Fine> returnBook(int borrowRecordId) {
        BorrowRecord record = borrowDAO.findById(borrowRecordId)
            .orElseThrow(() -> new IllegalArgumentException("Phiếu mượn không tồn tại."));
        if (record.getStatus() == BorrowRecord.Status.RETURNED)
            throw new IllegalStateException("Sách này đã được trả rồi.");

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowRecord.Status.RETURNED);
        borrowDAO.update(record);
        bookDAO.updateAvailableCopies(record.getBookId(), +1);

        Fine fine = null;
        long overdueDays = record.getOverdueDays();
        if (overdueDays > 0) {
            BigDecimal fineAmount = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
            fine = new Fine(record.getId(), fineAmount,
                "Trả sách trễ " + overdueDays + " ngày (" + FINE_PER_DAY.toPlainString() + "đ/ngày)");
            fineDAO.save(fine);
        }
        return Optional.ofNullable(fine);
    }

    /**
     * Marks a borrow record as LOST.
     * Reduces total_copies for the book and creates a replacement fee fine.
     */
    public Fine markAsLost(int borrowRecordId) {
        BorrowRecord record = borrowDAO.findById(borrowRecordId)
            .orElseThrow(() -> new IllegalArgumentException("Phiếu mượn không tồn tại."));
        if (record.getStatus() == BorrowRecord.Status.RETURNED)
            throw new IllegalStateException("Phiếu mượn đã được đóng.");
        record.setStatus(BorrowRecord.Status.LOST);
        borrowDAO.update(record);
        // The physical copy is permanently gone — reduce total_copies
        bookDAO.decreaseTotalCopies(record.getBookId());
        // Create a fixed replacement fine
        Fine fine = new Fine(record.getId(), LOST_BOOK_FEE, "Mất sách - phí bồi thường 200,000đ");
        fineDAO.save(fine);
        return fine;
    }

    public int syncOverdueStatuses() {
        return borrowDAO.markOverdueRecords();
    }

    public long countActiveBorrows() {
        return borrowDAO.countActiveBorrows();
    }

    public long countOverdue() {
        return borrowDAO.countOverdue();
    }
}
