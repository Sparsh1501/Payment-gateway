package com.paygateway.checkout;

import com.paygateway.dto.checkout.LineItem;
import com.paygateway.entity.CheckoutSession;
import com.paygateway.entity.Merchant;
import com.paygateway.entity.enums.CheckoutStatus;
import com.paygateway.service.CheckoutService;
import com.paygateway.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Serves the merchant-branded hosted checkout page and handles its submission.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CheckoutPageController {

    private final CheckoutService checkoutService;
    private final MerchantService merchantService;

    @GetMapping("/checkout/{sessionId}")
    public String renderCheckout(@PathVariable UUID sessionId, Model model) {
        CheckoutSession session = checkoutService.getForDisplay(sessionId);
        Merchant merchant = merchantService.getById(session.getMerchantId());
        List<LineItem> items = checkoutService.parseLineItems(session);
        BigDecimal total = items.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("businessName", merchant.getBusinessName());
        model.addAttribute("lineItems", items);
        model.addAttribute("total", total);
        model.addAttribute("currency", checkoutService.currencyOf(session));
        model.addAttribute("status", session.getStatus().name());
        model.addAttribute("expired", session.getStatus() == CheckoutStatus.EXPIRED);
        model.addAttribute("completed", session.getStatus() == CheckoutStatus.COMPLETED);
        return "checkout";
    }

    @PostMapping("/checkout/{sessionId}")
    public String submitCheckout(@PathVariable UUID sessionId, Model model) {
        try {
            String redirectUrl = checkoutService.complete(sessionId);
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            log.warn("Checkout submission failed for {}: {}", sessionId, e.getMessage());
            model.addAttribute("error", e.getMessage());
            return renderCheckout(sessionId, model);
        }
    }
}
