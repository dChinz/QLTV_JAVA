package com.example.service;

import com.example.dao.MemberDAO;
import com.example.model.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MemberService {

    private final MemberDAO memberDAO;

    public MemberService() {
        this.memberDAO = new MemberDAO();
    }

    /** Package-private constructor for testing. */
    MemberService(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public List<Member> getAllMembers() {
        return memberDAO.findAll();
    }

    public List<Member> searchMembers(String keyword) {
        if (keyword == null || keyword.isBlank()) return memberDAO.findAll();
        return memberDAO.search(keyword.trim());
    }

    public Optional<Member> getMemberById(int id) {
        return memberDAO.findById(id);
    }

    public Optional<Member> getMemberByCode(String code) {
        return memberDAO.findByCode(code);
    }

    public void addMember(Member member) {
        validateMember(member);
        member.setMemberCode(memberDAO.generateNextMemberCode());
        member.setJoinDate(LocalDate.now());
        if (member.getExpiryDate() == null)
            member.setExpiryDate(LocalDate.now().plusYears(2));
        member.setStatus(Member.Status.ACTIVE);
        memberDAO.save(member);
    }

    public void updateMember(Member member) {
        validateMember(member);
        memberDAO.findById(member.getId())
            .orElseThrow(() -> new IllegalArgumentException("Đọc giả không tồn tại."));
        memberDAO.update(member);
    }

    /** Smoke delete: đánh dấu ẩn, không xóa hàng DB (giữ lịch sử mượn). */
    public void deleteMember(int id) {
        memberDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Đọc giả không tồn tại hoặc đã được ẩn."));
        memberDAO.softDelete(id);
    }

    public void renewCard(int memberId, int years) {
        Member member = memberDAO.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Đọc giả không tồn tại."));
        LocalDate newExpiry = member.getExpiryDate().isAfter(LocalDate.now())
            ? member.getExpiryDate().plusYears(years)
            : LocalDate.now().plusYears(years);
        member.setExpiryDate(newExpiry);
        member.setStatus(Member.Status.ACTIVE);
        memberDAO.update(member);
    }

    public long getTotalMembers() {
        return memberDAO.countTotal();
    }

    /** Auto-update expired member statuses */
    public void syncExpiredStatuses() {
        List<Member> all = memberDAO.findAll();
        for (Member m : all) {
            if (m.getStatus() == Member.Status.ACTIVE
                    && m.getExpiryDate() != null
                    && LocalDate.now().isAfter(m.getExpiryDate())) {
                m.setStatus(Member.Status.EXPIRED);
                memberDAO.update(m);
            }
        }
    }

    private void validateMember(Member member) {
        if (member.getFullName() == null || member.getFullName().isBlank())
            throw new IllegalArgumentException("Họ tên đọc giả không được để trống.");
    }
}
