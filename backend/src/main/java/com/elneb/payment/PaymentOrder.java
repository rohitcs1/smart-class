package com.elneb.payment;
import com.elneb.common.UuidEntitySupport;
import com.elneb.subscription.SubscriptionPlan;
import com.elneb.tenant.Tenant;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="payment_orders")
public class PaymentOrder extends UuidEntitySupport {
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="tenant_id") private Tenant tenant;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="plan_id") private SubscriptionPlan plan;
    @Column(name="razorpay_order_id",nullable=false,unique=true,length=100) private String razorpayOrderId;
    @Column(name="razorpay_payment_id",length=100) private String razorpayPaymentId;
    @Column(name="amount_paise",nullable=false) private long amountPaise;
    @Column(nullable=false,length=30) private String status;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Column(name="paid_at") private Instant paidAt;
    protected PaymentOrder(){}
    public PaymentOrder(Tenant tenant,SubscriptionPlan plan,String orderId,long amount){this.tenant=tenant;this.plan=plan;this.razorpayOrderId=orderId;this.amountPaise=amount;this.status="CREATED";}
    public String getRazorpayOrderId(){return razorpayOrderId;} public SubscriptionPlan getPlan(){return plan;} public Tenant getTenant(){return tenant;} public String getStatus(){return status;}
    public void markPaid(String paymentId){razorpayPaymentId=paymentId;status="PAID";paidAt=Instant.now();}
}
