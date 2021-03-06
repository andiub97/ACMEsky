package com.ACMEbank.BankService.controller;

import java.util.List;

import javax.validation.Valid;

import com.ACMEbank.BankService.dto.PaymentLinkRequestDTO;
import com.ACMEbank.BankService.dto.PaymentRequestDTO;
import com.ACMEbank.BankService.dto.DepositRequestDTO;
import com.ACMEbank.BankService.dto.LinkResponseDTO;
import com.ACMEbank.BankService.model.WaitingPayment;
import com.ACMEbank.BankService.service.PaymentService;
import com.ACMEbank.BankService.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller che effettua la gestione dei pagamenti
 * Giacomo Vallorani 
 * giacomo.vallorani4@studio.unibo.it
 */
@RestController
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @Autowired
    UserService userService;
    
    @Autowired
    private Environment env;

    /**
     * Richiesta creazione link pagamento
     * @param paymentDTO oggetto di richiesta
     * @return  rispsota con link e token
     */
    @PostMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<LinkResponseDTO> savePayment(@Valid @RequestBody PaymentLinkRequestDTO paymentDTO) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String token = paymentService.savePayment(paymentDTO, userId);
        String path = env.getProperty("server.path") + "/link/index.html?token="+token;
        LinkResponseDTO response = new LinkResponseDTO(path, token);
        return new ResponseEntity<LinkResponseDTO>(response, HttpStatus.CREATED);
    }

    /**
     * Richiesta di tutti i link generati dall'utente
     * @return lista di link
     */
    @GetMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<List<WaitingPayment>> getAllPayments() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<WaitingPayment> paymentList = paymentService.getAllPayments(userId);
        return new ResponseEntity<List<WaitingPayment>>(paymentList, HttpStatus.OK);
    }

    /**
     * Deposito di denaro sul conto
     * @param depositRequestDTO oggetto di richiesta deposito
     */
    @PostMapping(value = "/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    private void deposit(@Valid @RequestBody DepositRequestDTO depositRequestDTO) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.deposit(depositRequestDTO, userId);
    }

    /**
     * Invia un pagamento
     * @param payment oggetto di richiesta pagamento
     */
    @PostMapping(value = "/pay", produces = MediaType.APPLICATION_JSON_VALUE)
    private void pay(@Valid @RequestBody PaymentRequestDTO payment) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.pay(payment, userId);
    }

    /**
     * Utilizzato dalla pagina web per richiedere il pagamento in base al token
     * @return pagamento
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/payments/{payment_token}")
    private ResponseEntity<WaitingPayment> getPayment(@PathVariable("payment_token") String paymentToken) {
        WaitingPayment payment = paymentService.getPaymentByToken(paymentToken);
        if(payment == null || payment.getPayed() == true)
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        return new ResponseEntity<WaitingPayment>(payment, HttpStatus.OK);
    }

    /**
     * Utilizzato dalla pagina web per "informare" dell'avvenuto pagamento
     * @param paymentToken token del pagamento
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @PutMapping("/payments/{payment_token}")
    private void payLink(@PathVariable("payment_token") String paymentToken) {
        // Non è stata gestita la concorrenza
        WaitingPayment payment = paymentService.getPaymentByToken(paymentToken);
        payment.setPayed(true);
        paymentService.updatePayment(payment);
        userService.deposit(new DepositRequestDTO(payment.getAmount()), payment.getUserId());
        paymentService.sendPaymentNotification(payment.getNotificationUrl(), payment.getPaymentToken());
    }
}