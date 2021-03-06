package com.ACMEbank.BankService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.ArrayList;

import com.ACMEbank.BankService.model.WaitingPayment;

/**
 * Interfaccia tabella waiting_payment
 * Giacomo Vallorani 
 * giacomo.vallorani4@studio.unibo.it
 */
public interface WaitingPaymentRepository extends JpaRepository<WaitingPayment, Long> {

    @Query(value = "SELECT * FROM WAITING_PAYMENTS p WHERE p.USER_ID = ?1", nativeQuery = true)
    ArrayList<WaitingPayment> getAllPayments(String userId);

    @Query(value = "SELECT * FROM WAITING_PAYMENTS p WHERE p.PAYMENT_TOKEN = ?1", nativeQuery = true)
    WaitingPayment getPaymentByToken(String paymentToken);

}