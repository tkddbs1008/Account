package com.example.account.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountUserRepository accountUserRepository;
	private final AccountRepository accountRepository;
	
	@Transactional
	public TransactionDto useBalance(
			Long userId, String accountNumber, Long amount
	){
		AccountUser user = accountUserRepository.findById(userId)
				.orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
		Account account = accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
		validateUseBalance(user, account, amount);
		
		account.useBalance(amount);
		
		return TransactionDto.fromEntity(saveAndGetTransaction(TransactionType.USE,
				TransactionResultType.S, amount, account));	
	}
	
	private void validateUseBalance(AccountUser user, Account account, Long amount) {
		if(!Objects.equals(user.getId(), account.getAccountUser().getId())) {
			throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
		}
		if(account.getAccountStatus() != AccountStatus.IN_USE) {
			throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
		}
		if(account.getBalance() < amount) {
			throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
		}
	}
	
	@Transactional
	public void saveFailedUseTransaction(String accountNumber, Long amount) {
		Account account = accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
		
		saveAndGetTransaction(TransactionType.USE, TransactionResultType.F, amount, account);
	} 
	
	private Transaction saveAndGetTransaction(TransactionType transactionType,
			TransactionResultType transactionResultType, 
			Long amount, 
			Account account) {
		return transactionRepository.save(
				Transaction.builder()
				.transactionType(TransactionType.USE)
				.transactionResultType(transactionResultType)
				.account(account)
				.amount(amount)
				.balanceSnapshot(account.getBalance())
				.transactionId(UUID.randomUUID().toString().replace("-",""))
				.transactedAt(LocalDateTime.now())
				.build());			
	}

	@Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account,amount);
        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.S, amount, account));
    }
	
	private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if(!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if(!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }
        if(transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }
	
	@Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.F, amount, account);
    }
	
	public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(
                transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
