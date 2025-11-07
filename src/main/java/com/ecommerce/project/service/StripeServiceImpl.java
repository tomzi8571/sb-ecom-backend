package com.ecommerce.project.service;

import com.ecommerce.project.payload.StripePaymentDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    // Set your secret key. Remember to switch to your live secret key in production.
    // See your keys here: https://dashboard.stripe.com/apikeys
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentIntent paymentIntent(StripePaymentDto stripePaymentDto) throws StripeException {
        CustomerSearchParams searchParams =
                CustomerSearchParams.builder()
                        .setQuery(String.format("email:'%s'", stripePaymentDto.getEmail()))
                        .build();
        CustomerSearchResult customers = Customer.search(searchParams);

        Customer customer = createOrUpdateCustomerInfo(stripePaymentDto, customers);

        PaymentIntentCreateParams params = createPaymentAndShippingInfo(stripePaymentDto, customer);

        return PaymentIntent.create(params);
    }

    // Update Payment info, Shipping Address, Tracking information
    private static PaymentIntentCreateParams createPaymentAndShippingInfo(StripePaymentDto stripePaymentDto, Customer customer) {
        return PaymentIntentCreateParams.builder()
                .setAmount(stripePaymentDto.getAmount())
                .setCurrency(stripePaymentDto.getCurrency())
                .setCustomer(customer.getId())
                .setShipping(
                        PaymentIntentCreateParams.Shipping.builder()
                                .setAddress(
                                        PaymentIntentCreateParams.Shipping.Address.builder()
                                                .setLine1(stripePaymentDto.getAddress().getStreet())
                                                .setCity(stripePaymentDto.getAddress().getCity())
                                                .setState(stripePaymentDto.getAddress().getState())
                                                .setPostalCode(stripePaymentDto.getAddress().getPincode())
                                                .setCountry(stripePaymentDto.getAddress().getCountry())
                                                .build()
                                )
                                .setName(stripePaymentDto.getName())
                                .setCarrier("Test Carrier")
                                .setTrackingNumber("Test Tracking Number: 12335324513123")
                                .build()
                )
                .setDescription(stripePaymentDto.getDescription())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();
    }

    // Create or update customer with default billing and shipping address
    private static Customer createOrUpdateCustomerInfo(StripePaymentDto stripePaymentDto, CustomerSearchResult customers) throws StripeException {
        Customer customer;
        if (customers.getData().isEmpty()) {
            CustomerCreateParams createParams =
                    CustomerCreateParams.builder()
                            .setName(stripePaymentDto.getName())
                            .setEmail(stripePaymentDto.getEmail())
                            .build();
            customer = Customer.create(createParams);
        } else {
            customer = customers.getData().get(0);
        }

        customer.update(CustomerUpdateParams.builder()
                // Billing address, also Used for taxes
                .setAddress(
                        CustomerUpdateParams.Address.builder()
                                .setLine1(stripePaymentDto.getAddress().getStreet())
                                .setCity(stripePaymentDto.getAddress().getCity())
                                .setState(stripePaymentDto.getAddress().getState())
                                .setPostalCode(stripePaymentDto.getAddress().getPincode())
                                .setCountry(stripePaymentDto.getAddress().getCountry())
                                .build())
                //  Shipping Address, used for the invoice
                .setShipping(
                        CustomerUpdateParams.Shipping.builder()
                                .setAddress(
                                        CustomerUpdateParams.Shipping.Address.builder()
                                                .setLine1(stripePaymentDto.getAddress().getStreet())
                                                .setCity(stripePaymentDto.getAddress().getCity())
                                                .setState(stripePaymentDto.getAddress().getState())
                                                .setPostalCode(stripePaymentDto.getAddress().getPincode())
                                                .setCountry(stripePaymentDto.getAddress().getCountry())
                                                .build()
                                )
                                .setName(stripePaymentDto.getName())
                                .build())
                .build());
        return customer;
    }
}
