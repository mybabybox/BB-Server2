package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.ReportedPost;

public class ReportedPostVM {
	@JsonProperty("id") public Long id;
	@JsonProperty("createdDate") public Long createdDate;
	@JsonProperty("postId") public Long postId;
	@JsonProperty("reporterId") public Long reporterId;
	@JsonProperty("body") public String body;
	@JsonProperty("note") public String note;
	@JsonProperty("reportedType") public String reportedType;
	
    public ReportedPostVM(ReportedPost reportedPost) {
        this.id = reportedPost.id;
        this.createdDate = reportedPost.getCreatedDate().getTime();
        this.postId = reportedPost.postId;
        this.reporterId = reportedPost.reporterId;
        this.body = reportedPost.body;
        this.note = reportedPost.note;
        this.reportedType = reportedPost.reportedType.name();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReportedType() {
        return reportedType;
    }

    public void setReportedType(String reportedType) {
        this.reportedType = reportedType;
    }
}