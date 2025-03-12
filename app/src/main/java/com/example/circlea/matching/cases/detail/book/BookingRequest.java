    package com.example.circlea.matching.cases.detail.book;

    import java.util.Calendar;

    public class BookingRequest {
        private String requestId;
        private String studentId;
        private String studentName;
        private Calendar dateTime;
        private String status;
        private String slotId;
        private Calendar startTime;
        private Calendar endTime;

        public BookingRequest(String requestId, String studentId, String studentName,
                              Calendar startTime, Calendar endTime, String status) {
            this.requestId = requestId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
        }

        // Getters
        public String getRequestId() {
            return requestId;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public Calendar getDateTime() {
            return startTime;
        }

        public Calendar getStartTime() {
            return startTime;
        }

        public Calendar getEndTime() {
            return endTime;
        }

        public String getStatus() {
            return status;
        }

        public String getSlotId() {
            return slotId;
        }

        // Setters
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public void setStartTime(Calendar startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(Calendar endTime) {
            this.endTime = endTime;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setSlotId(String slotId) {
            this.slotId = slotId;
        }

        // Helper method to check if the request is pending
        public boolean isPending() {
            return "pending".equalsIgnoreCase(status);
        }

        // Helper method to check if the request is confirmed
        public boolean isConfirmed() {
            return "confirmed".equalsIgnoreCase(status);
        }

        // Helper method to check if the request is rejected
        public boolean isRejected() {
            return "rejected".equalsIgnoreCase(status);
        }
    }