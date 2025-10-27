package fr.ecommerce.flore.aroma.flore_aroma.account.mappers;

import fr.ecommerce.flore.aroma.flore_aroma.account.bean.Account;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.AccountDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    AccountDTO fromAccount(Account account);

    Account fromAccountDTO(AccountDTO accountDTO);
}
