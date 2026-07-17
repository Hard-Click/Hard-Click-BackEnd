package com.wanted.backend.domain.cource.domain.model;

import com.wanted.backend.global.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Course {

    private Long id;
    private Long authorId;
    private String title;
    private String subject;
    private String description;
    private String thumbnailUrl;
    private PriceType priceType;
    private int price;
    private CourseStatus status;
    private List<CourseSection> sections;
    private Instant createdAt;
    private List<String> learningObjectives;
    private List<String> targetAudience;
    private List<String> techTags;
    private String level;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Course() {}

    public static Course create(Long authorId, String title, String subject,
                                String description, String thumbnailUrl,
                                PriceType priceType, int price,
                                List<CourseSection> sections, Instant now,
                                List<String> learningObjectives, List<String> targetAudience,
                                List<String> techTags, String level) {
        validatePrice(priceType, price);
        Course course = new Course();
        course.authorId = authorId;
        course.title = title;
        course.subject = subject;
        course.description = description;
        course.thumbnailUrl = thumbnailUrl;
        course.priceType = priceType;
        course.price = price;
        course.status = CourseStatus.DRAFT;
        course.sections = new ArrayList<>(sections);
        course.createdAt = now;
        course.learningObjectives = learningObjectives;
        course.targetAudience = targetAudience;
        course.techTags = techTags;
        course.level = level;
        return course;
    }

    public static Course restore(Long id, Long authorId, String title, String subject,
                                 String description, String thumbnailUrl,
                                 PriceType priceType, int price, CourseStatus status,
                                 List<CourseSection> sections, Instant createdAt,
                                 List<String> learningObjectives, List<String> targetAudience,
                                 List<String> techTags, String level) {
        Course course = new Course();
        course.id = id;
        course.authorId = authorId;
        course.title = title;
        course.subject = subject;
        course.description = description;
        course.thumbnailUrl = thumbnailUrl;
        course.priceType = priceType;
        course.price = price;
        course.status = status;
        course.sections = new ArrayList<>(sections);
        course.createdAt = createdAt;
        course.learningObjectives = learningObjectives;
        course.targetAudience = targetAudience;
        course.techTags = techTags;
        course.level = level;
        return course;
    }

    // 강의 기본 정보 + 커리큘럼 수정. description은 입력받지 않으므로 기존 값을 그대로 둔다.
    public void update(String title, String subject,
                       String thumbnailUrl, PriceType priceType, int price,
                       List<CourseSection> sections,
                       List<String> learningObjectives, List<String> targetAudience,
                       List<String> techTags, String level) {
        ensureNotDeleted();
        validatePrice(priceType, price);
        this.title = title;
        this.subject = subject;
        this.thumbnailUrl = thumbnailUrl;
        this.priceType = priceType;
        this.price = price;
        this.sections = new ArrayList<>(sections);
        this.learningObjectives = learningObjectives;
        this.targetAudience = targetAudience;
        this.techTags = techTags;
        this.level = level;
    }

    public void updateThumbnail(String thumbnailUrl) {
        ensureNotDeleted();
        this.thumbnailUrl = thumbnailUrl;
    }

    public void softDelete() {
        this.status = CourseStatus.DELETED;
    }

    // 강의 공개
    public void publish() {
        ensureNotDeleted();
        this.status = CourseStatus.PUBLISHED;
    }

    // 강의 비공개
    public void unpublish() {
        ensureNotDeleted();
        this.status = CourseStatus.DRAFT;
    }

    // 삭제(DELETED)된 강의는 수정·상태변경 불가
    private void ensureNotDeleted() {
        if (this.status == CourseStatus.DELETED) {
            throw new IllegalStateException("삭제된 강의는 변경할 수 없습니다.");
        }
    }

    public boolean isDeleted() {
        return this.status == CourseStatus.DELETED;
    }

    private static void validatePrice(PriceType priceType, int price) {
        if (priceType == PriceType.FREE && price != 0) {
            throw new InvalidCoursePriceException("강의 가격 설정이 올바르지 않습니다.");
        }
        if (priceType == PriceType.PAID && price <= 0) {
            throw new InvalidCoursePriceException("강의 가격 설정이 올바르지 않습니다.");
        }
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public PriceType getPriceType() { return priceType; }
    public int getPrice() { return price; }
    public CourseStatus getStatus() { return status; }
    public List<CourseSection> getSections() { return List.copyOf(sections); }
    public Instant getCreatedAt() { return createdAt; }
    public List<String> getLearningObjectives() { return learningObjectives != null ? List.copyOf(learningObjectives) : List.of(); }
    public List<String> getTargetAudience() { return targetAudience != null ? List.copyOf(targetAudience) : List.of(); }
    public List<String> getTechTags() { return techTags != null ? List.copyOf(techTags) : List.of(); }
    public String getLevel() { return level; }
}
