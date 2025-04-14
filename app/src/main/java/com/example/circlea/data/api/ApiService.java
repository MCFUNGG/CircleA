package com.example.circlea.data.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @FormUrlEncoded
    @POST("get_tutor_contact.php")
    Call<ResponseBody> getTutorContact(
            @Field("tutor_id") String tutorId
    );
    
    @FormUrlEncoded
    @POST("get_payment_status.php")
    Call<ResponseBody> getPaymentStatus(
            @Field("match_id") String matchId,
            @Field("student_id") String studentId
    );
    
    @Multipart
    @POST("process_payment.php")
    Call<ResponseBody> processPayment(
            @Part("match_id") RequestBody matchId,
            @Part("student_id") RequestBody studentId,
            @Part("amount") RequestBody amount,
            @Part MultipartBody.Part receipt
    );
    
    @FormUrlEncoded
    @POST("get_student_contact.php")
    Call<ResponseBody> getStudentContact(
            @Field("match_id") String matchId,
            @Field("tutor_id") String tutorId
    );
    
    // Add other API methods here
} 