package com.seastar.entity;

import java.util.ArrayList;

/**
 * Created by os on 16-6-4.
 * 苹果验证返回数据结构
 */
public class AppleIapReceipt {

    public String environment = "";

    // Either 0 if the receipt is valid, or one of the error codes listed in Table 2-1.
    // For iOS 6 style transaction receipts, the status code reflects the status of the specific transaction’s receipt.
    // For iOS 7 style app receipts, the status code is reflects the status of the app receipt as a whole. For example,
    // if you send a valid app receipt that contains an expired subscription, the response is 0 because the receipt as
    // a whole is valid.

    // Table 2-1
    // 21000 The App Store could not read the JSON object you provided.
    // 21002 The data in the receipt-data property was malformed or missing.
    // 21003 The receipt could not be authenticated.
    // 21004 The shared secret you provided does not match the shared secret on file for your account.
    //       Only returned for iOS 6 style transaction receipts for auto-renewable subscriptions.
    // 21005 The receipt server is not currently available.
    // 21006 This receipt is valid but the subscription has expired. When this status code is returned to your server,
    //       the receipt data is also decoded and returned as part of the response.Only returned for iOS 6 style
    //       transaction receipts for auto-renewable subscriptions.
    // 21007 This receipt is from the test environment, but it was sent to the production environment for verification.
    //       Send it to the test environment instead.
    // 21008 This receipt is from the production environment, but it was sent to the test environment for verification.
    //       Send it to the production environment instead.
    public int status = 0;

    // A JSON representation of the receipt that was sent for verification. For information about keys found in a receipt,
    // see Receipt Fields.
    public Receipt receipt = new Receipt();

    // Only returned for iOS 6 style transaction receipts for auto-renewable subscriptions. The base-64 encoded transaction
    // receipt for the most recent renewal.
    public String latest_receipt = "";


    // Only returned for iOS 6 style transaction receipts for auto-renewable subscriptions. The JSON representation of
    // the receipt for the most recent renewal
    public ArrayList<InApp> latest_receipt_info = new ArrayList<>();


    public static class Receipt {
        public long adam_id = 0;
        public long app_item_id = 0;

        // The app’s bundle identifier.
        // This corresponds to the value of CFBundleIdentifier in the Info.plist file.
        public String bundle_id = "";

        // The app’s version number.
        // This corresponds to the value of CFBundleVersion (in iOS) or CFBundleShortVersionString (in OS X) in the Info.plist.
        public String application_version = "";
        public long download_id = 0;

        // The version of the app that was originally purchased.
        // This corresponds to the value of CFBundleVersion (in iOS) or CFBundleShortVersionString (in OS X) in the
        // Info.plist file when the purchase was originally made.
        // In the sandbox environment, the value of this field is always “1.0”.
        // Receipts prior to June 20, 2013 omit this field. It is populated on all new receipts, regardless of OS version.
        // If you need the field but it is missing, manually refresh the receipt using the SKReceiptRefreshRequest class.
        public String original_application_version = "";

        // The date when the app receipt was created.
        // When validating a receipt, use this date to validate the receipt’s signature.
        public String original_purchase_date = "";
        public String original_purchase_date_ms = "";
        public String original_purchase_date_pst = "";

        // The date when the app receipt was created.
        // When validating a receipt, use this date to validate the receipt’s signature.
        public String receipt_creation_date = "";
        public long receipt_creation_date_ms = 0;
        public String receipt_creation_date_pst = "";

        // 是否沙箱
        public String receipt_type = "";

        public String request_date = "";
        public String request_date_ms = "";
        public String request_date_pst = "";

        public int version_external_identifier = 0;

        // The receipt for an in-app purchase.
        // In the JSON file, the value of this key is an array containing all in-app purchase receipts. In the ASN.1 file,
        // there are multiple fields that all have type 17, each of which contains a single in-app purchase receipt.
        // The in-app purchase receipt for a consumable product or non-renewing subscription is added to the receipt when
        // the purchase is made. It is kept in the receipt until your app finishes that transaction. After that point, it
        // is removed from the receipt the next time the receipt is updated—for example, when the user makes another
        // purchase or if your app explicitly refreshes the receipt.
        // The in-app purchase receipt for a non-consumable product, auto-renewable subscription, or free subscription
        // remains in the receipt indefinitely.
        public ArrayList<InApp> in_app = new ArrayList<>();

        // 以下兼容ios6
        public String purchase_date_ms = "";
        public String unique_identifier = "";
        public String original_transaction_id = "";
        public String bvrs = "";
        public String transaction_id = "";
        public String quantity = "";
        public String unique_vendor_identifier = "";
        public String item_id = "";
        public String product_id = "";
        public String purchase_date = "";
        public String purchase_date_pst = "";
        public String bid = "";
    }


    public static class InApp {
        // The number of items purchased.
        // This value corresponds to the quantity property of the SKPayment object stored in the transaction’s payment property.
        public String quantity = "";

        // The product identifier of the item that was purchased.
        // This value corresponds to the productIdentifier property of the SKPayment object stored in the transaction’s
        // payment property.
        public String product_id = "";

        // The transaction identifier of the item that was purchased.
        // This value corresponds to the transaction’s transactionIdentifier property
        public String transaction_id = "";

        // For a transaction that restores a previous transaction, the transaction identifier of the original transaction.
        // Otherwise, identical to the transaction identifier.
        // This value corresponds to the original transaction’s transactionIdentifier property.
        // All receipts in a chain of renewals for an auto-renewable subscription have the same value for this field.
        public String original_transaction_id = "";

        // For a transaction that restores a previous transaction, the date of the original transaction.
        // This value corresponds to the original transaction’s transactionDate property
        // In an auto-renewable subscription receipt, this indicates the beginning of the subscription period, even if
        // the subscription has been renewed.
        public String original_purchase_date = "";
        public String original_purchase_date_pst = "";
        public String original_purchase_date_ms = "";



        public String is_trial_period = "";

        // A string that the App Store uses to uniquely identify the application that created the transaction.
        // If your server supports multiple applications, you can use this value to differentiate between them.
        // Apps are assigned an identifier only in the production environment, so this key is not present for receipts
        // created in the test environment.
        public String app_item_id = "";

        // An arbitrary number that uniquely identifies a revision of your application
        // This key is not present for receipts created in the test environment.
        public String version_external_identifier = "";

        // The primary key for identifying subscription purchases
        // INTEGER
        public String web_order_line_item_id = "";

        // The date and time that the item was purchased.
        // This value corresponds to the transaction’s transactionDate property.
        // For a transaction that restores a previous transaction, the purchase date is the same as the original
        // purchase date. Use Original Purchase Date to get the date of the original transaction.
        // In an auto-renewable subscription receipt, this is always the date when the subscription was purchased or
        // renewed, regardless of whether the transaction has been restored.
        public String purchase_date = "";
        public String purchase_date_ms = "";
        public String purchase_date_pst = "";

        // The expiration date for the subscription, expressed as the number of milliseconds since January 1, 1970, 00:00:00 GMT.
        // This key is only present for auto-renewable subscription receipts.
        // Number
        public String expires_date = "";
        public String expires_date_ms = "";
        public String expires_date_pst = "";

        // For a transaction that was canceled by Apple customer support, the time and date of the cancellation.
        // Treat a canceled receipt the same as if no purchase had ever been made.
        public String cancellation_date = "";
        public String cancellation_date_ms = "";
        public String cancellation_date_pst = "";
    }
}
