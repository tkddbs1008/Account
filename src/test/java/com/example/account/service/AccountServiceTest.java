package com.example.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import org.mockito.junit.jupiter.MockitoExtension;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
	
	@Mock
	private AccountRepository accountRepository;
	
	
	@Mock
	private AccountUserRepository accountUserRepository;
	
	@InjectMocks
	private AccountService accountService;
	
	@Test
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        		user.setId(12L);	
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                                .accountUser(user)
                                .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L,1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());

    }
	
	@Test
	void successGetAccountsByUserId(){
		//given
		AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        		user.setId(12L);	

		List<Account> accounts = Arrays.asList(
				Account.builder()
						.accountUser(user)
						.accountNumber("1234567890")
						.balance(1000L)
						.build(),
				Account.builder()
						.accountUser(user)
						.accountNumber("1234567891")
						.balance(2000L)
						.build(),
				Account.builder()
						.accountUser(user)
						.accountNumber("1234567892")
						.balance(3000L)
						.build()
		);
		
		given(accountUserRepository.findById(anyLong()))
        		.willReturn(Optional.of(user));
		given(accountRepository.findByAccountUser(any()))
				.willReturn(accounts);
		//when
		List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);
		//then
		assertEquals(3, accountDtos.size());
		assertEquals("1234567890", accountDtos.get(0).getAccountNumber());
		assertEquals(1000, accountDtos.get(0).getBalance());
		assertEquals("1234567891", accountDtos.get(1).getAccountNumber());
		assertEquals(2000, accountDtos.get(1).getBalance());
		assertEquals("1234567892", accountDtos.get(2).getAccountNumber());
		assertEquals(3000, accountDtos.get(2).getBalance());
	}
	
	@Test
	@DisplayName("유저가 없는 아이디 입니다")
	void failedToGetAccounts(){
		//given
		given(accountUserRepository.findById(anyLong()))
			.willReturn(Optional.empty());
		//when
        AccountException exception = assertThrows(AccountException.class,
        		() -> accountService.getAccountsByUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
	}
	
	
	@Test
    void createFirstSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        		user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L,1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }
	
	@Test
	@DisplayName("해당 유저 없음 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
        		() -> accountService.createAccount(1L,1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
	
	@Test
	@DisplayName("유저 당 최대 계좌는 10개")
	void createAccount_maxAccountIs10(){
		//given
		AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        		user.setId(12L);
		given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
		given(accountRepository.countByAccountUser(any()))
				.willReturn(10);
		//when
        AccountException exception = assertThrows(AccountException.class,
        		() -> accountService.createAccount(1L,1000L));
        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
	}
}
