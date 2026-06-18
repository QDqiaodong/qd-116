package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryCheckRepository inventoryCheckRepository;

    public InventoryCheck check(String checkMonth, Integer totalBook, Integer totalActual, String checker, String remark) {
        Integer difference = totalActual - totalBook;
        InventoryCheck inventoryCheck = InventoryCheck.builder()
                .checkMonth(checkMonth)
                .totalBook(totalBook)
                .totalActual(totalActual)
                .difference(difference)
                .checker(checker)
                .checkTime(LocalDateTime.now())
                .remark(remark)
                .build();
        return inventoryCheckRepository.save(inventoryCheck);
    }

    @Transactional(readOnly = true)
    public List<InventoryCheck> listByMonth(String checkMonth) {
        return inventoryCheckRepository.findByCheckMonth(checkMonth)
                .map(List::of)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<InventoryCheck> listAll() {
        return inventoryCheckRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryCheck> getLatestCheck() {
        return inventoryCheckRepository.findTopByOrderByCheckTimeDesc();
    }
}
