package de.einfachhans.ContactsX;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import contacts.core.Contacts;
import contacts.core.ContactsFactory;
import contacts.core.Fields;
import contacts.core.Query;
import contacts.core.WhereKt;
import contacts.core.entities.Address;
import contacts.core.entities.Contact;
import contacts.core.entities.Email;
import contacts.core.entities.Name;
import contacts.core.entities.Organization;
import contacts.core.entities.Phone;
import contacts.core.entities.RawContact;

/**
 * This class echoes a string called from JavaScript.
 */
public class ContactsX extends CordovaPlugin {

    private CallbackContext _callbackContext;
    private final String LOG_TAG = "ContactsX";

    public static final String READ = Manifest.permission.READ_CONTACTS;
    public static final String WRITE = Manifest.permission.WRITE_CONTACTS;

    private static final String EMAIL_REGEXP = ".+@.+\\.+.+"; /* <anything>@<anything>.<anything>*/

    public static final int REQ_CODE_PERMISSIONS = 0;
    public static final int REQ_CODE_PICK = 2;

    private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private Contacts contactsFactory = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this._callbackContext = callbackContext;

        try {
            if (action.equals("find")) {
                if (PermissionHelper.hasPermission(this, READ)) {
                    this.find(args);
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if (action.equals("pick")) {
                if (PermissionHelper.hasPermission(this, READ)) {
                    this.pick();
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if (action.equals("save")) {
                if(PermissionHelper.hasPermission(this, WRITE)) {
                    this.save(args);
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if(action.equals("delete")) {
                if(PermissionHelper.hasPermission(this, WRITE)) {
                    this.delete(args);
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if (action.equals("hasPermission")) {
                this.hasPermission();
            } else if (action.equals("requestPermission")) {
                boolean write = args.optBoolean(0);
                this.requestPermission(write);
            } else {
                returnError(ContactsXErrorCodes.UnsupportedAction);
            }
        } catch (JSONException exception) {
            returnError(ContactsXErrorCodes.WrongJsonObject);
        } catch (Exception exception) {
            returnError(ContactsXErrorCodes.UnknownError, exception.getMessage());
        }

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        if (requestCode == REQ_CODE_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                final String contactId = intent.getData().getLastPathSegment();

                this.cordova.getThreadPool().execute(() -> {
                    Query dataQuery = getContactsFactory()
                            .query()
                            .where(WhereKt.equalTo(Fields.Contact.Id, contactId));

                    if (dataQuery.find().size() <= 0) {
                        returnError(ContactsXErrorCodes.UnknownError, "Error occurred while retrieving contact raw id");
                        return;
                    }

                    JSONObject contact = getContactByQuery(dataQuery);
                    if (contact != null) {
                        this._callbackContext.success(contact);
                    } else {
                        returnError(ContactsXErrorCodes.UnknownError);
                    }
                });
            } else {
                returnError(ContactsXErrorCodes.CanceledAction);
            }
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        this.hasPermission();
    }

    private void find(JSONArray args) throws JSONException {
        ContactsXFindOptions options = new ContactsXFindOptions(args.optJSONObject(0));

        this.cordova.getThreadPool().execute(() -> {
            Query dataQuery = getContactsFactory()
                    .query();

            try {
                JSONArray contacts = handleFindResult(dataQuery, options);
                this._callbackContext.success(contacts);
            } catch (Exception e) {
                returnError(ContactsXErrorCodes.UnknownError, e.getMessage());
            }
        });
    }

	private JSONArray handleFindResult(Query contactQuery, ContactsXFindOptions options) throws JSONException {
        // initialize array
        JSONArray jsContacts = new JSONArray();
        List<Contact> contacts = contactQuery.find();

        if (contacts.size() <= 0) {
            return jsContacts;
        }

        for (Contact contact : contacts) {
            JSONObject jsContact = new JSONObject();

            jsContact.put("id", String.valueOf(contact.getId()));
            if (options.displayName) {
                String displayName = contact.getDisplayNamePrimary();
                jsContact.put("displayName", displayName);
            }

            try {
                RawContact rawContact = contact.getRawContacts().get(0);
                Name name = rawContact != null ? rawContact.getName() : null;
                if (name != null) {
                    if (options.firstName) {
                        String firstName = name.getGivenName();
                        jsContact.put("firstName", firstName);
                    }
                    if (options.middleName) {
                        String middleName = name.getMiddleName();
                        jsContact.put("middleName", middleName);
                    }
                    if (options.familyName) {
                        String familyName = name.getFamilyName();
                        jsContact.put("familyName", familyName);
                    }
                }

                Organization organization = rawContact != null && rawContact.getOrganization() != null
                        ? rawContact.getOrganization() : null;
                if (options.organizationName && organization != null) {
                    String company = rawContact.getOrganization().getCompany();
                    jsContact.put("organizationName", company);
                }
            } catch (IllegalArgumentException ignored) {
            }

            if (options.phoneNumbers) {
                jsContact.put("phoneNumbers", phoneQuery(contact, options));
            }

            if (options.emails) {
                jsContact.put("emails", emailQuery(contact));
            }

            if (options.addresses) {
                jsContact.put("addresses", addressQuery(contact));
            }

            jsContacts.put(jsContact);
        }

        return jsContacts;
    }

   private JSONArray phoneQuery(Contact contact, ContactsXFindOptions options) throws JSONException {
        JSONArray phoneNumbers = new JSONArray();

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Phone phoneNumber : rawContact.getPhones()) {
                JSONObject phoneNumberObj = new JSONObject();
                try {
                    phoneNumberObj.put("id", String.valueOf(phoneNumber.getId()));
                    String phoneValue = phoneNumber.getNumber();
                    phoneNumberObj.put("value", phoneValue);
                    phoneNumberObj.put("normalized", getNormalizedPhoneNumber(phoneValue, options));
                    phoneNumberObj.put("type", getPhoneType(phoneNumber.getType().getValue()));
                    phoneNumbers.put(phoneNumberObj);
                } catch (NullPointerException e) {
                    LOG.e(LOG_TAG, e.getMessage(), e);
                }
            }
        }

        return phoneNumbers;
    }

    private JSONArray emailQuery(Contact contact) throws JSONException {
        JSONArray emails = new JSONArray();

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Email email : rawContact.getEmails()) {
                JSONObject emailObj = new JSONObject();
                try {
                    emailObj.put("id", String.valueOf(email.getId()));
                    emailObj.put("value", email.getAddress());
                    emailObj.put("type", getMailType(email.getType().getValue()));
                    emails.put(emailObj);
                } catch (NullPointerException e) {
                    LOG.e(LOG_TAG, e.getMessage(), e);
                }
            }
        }

        return emails;
    }

    private JSONArray addressQuery(Contact contact) throws JSONException {
        JSONArray addresses = new JSONArray();

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Address address : rawContact.getAddresses()) {
                JSONObject addressObj = new JSONObject();
                try {
                    addressObj.put("id", String.valueOf(address.getId()));
                    addressObj.put("type", getAddressType(address.getType().getValue()));
                    addressObj.put("streetAddress", address.getStreet());
                    addressObj.put("locality", address.getCity());
                    addressObj.put("region", address.getRegion());
                    addressObj.put("postalCode", address.getPostcode());
                    addressObj.put("country", address.getCountry());
                    addresses.put(addressObj);
                } catch (NullPointerException e) {
                    LOG.e(LOG_TAG, e.getMessage(), e);
                }
            }
        }

        return addresses;
    }

    private void pick() {
        this.cordova.getThreadPool().execute(() -> {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            this.cordova.startActivityForResult(this, contactPickerIntent, REQ_CODE_PICK);
        });
    }

    private String getNormalizedPhoneNumber(String phoneNumber, ContactsXFindOptions options){
        if(options.baseCountryCode != null && phoneNumber != null){
            try {
                Phonenumber.PhoneNumber phoneNumberProto = phoneUtil.parse(phoneNumber, options.baseCountryCode);
                return phoneUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                return "";
            }
        }
        return "";
    }

    @Nullable
    private JSONObject getContactByQuery(Query query) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("phoneNumbers", true);
        fields.put("emails", true);
        fields.put("addresses", true);
        Map<String, Object> pickFields = new HashMap<>();
        pickFields.put("fields", fields);

        try {
            JSONArray contacts = handleFindResult(query, new ContactsXFindOptions(new JSONObject(pickFields)));
            if (contacts.length() == 1) {
                return contacts.getJSONObject(0);
            }
        } catch (Exception e) {
            returnError(ContactsXErrorCodes.UnknownError, e.getMessage());
        }

        return null;
    }

    @Nullable
    private JSONObject getContactById(String id) {
        Query dataQuery = getContactsFactory()
                .query()
                .where(WhereKt.equalTo(Fields.Contact.Id, id));

        Map<String, Object> fields = new HashMap<>();
        fields.put("phoneNumbers", true);
        fields.put("emails", true);
        fields.put("addresses", true);
        Map<String, Object> pickFields = new HashMap<>();
        pickFields.put("fields", fields);

        try {
            JSONArray contacts = handleFindResult(dataQuery, new ContactsXFindOptions(new JSONObject(pickFields)));
            if (contacts.length() == 1) {
                return contacts.getJSONObject(0);
            }
        } catch (Exception e) {
            returnError(ContactsXErrorCodes.UnknownError, e.getMessage());
        }

        return null;
    }

    private void save(JSONArray args) throws JSONException {
        final JSONObject contact = args.getJSONObject(0);
        this.cordova.getThreadPool().execute(() -> {
            JSONObject res = null;
            String id = performSave(contact);
            if (id != null) {
                res = getContactById(id);
            }
            if (res != null) {
                _callbackContext.success(res);
            } else {
                returnError(ContactsXErrorCodes.UnknownError);
            }
        });
    }

    @Nullable
    private String performSave(JSONObject contact) {
        AccountManager mgr = AccountManager.get(this.cordova.getActivity());
        Account[] accounts = mgr.getAccounts();
        String accountName = null;
        String accountType = null;

        if (accounts.length == 1) {
            accountName = accounts[0].name;
            accountType = accounts[0].type;
        } else if (accounts.length > 1) {
            for (Account a : accounts) {
                if (a.type.contains("eas") && a.name.matches(EMAIL_REGEXP)) /*Exchange ActiveSync*/ {
                    accountName = a.name;
                    accountType = a.type;
                    break;
                }
            }
            if (accountName == null) {
                for (Account a : accounts) {
                    if (a.type.contains("com.google") && a.name.matches(EMAIL_REGEXP)) /*Google sync provider*/ {
                        accountName = a.name;
                        accountType = a.type;
                        break;
                    }
                }
            }
            if (accountName == null) {
                for (Account a : accounts) {
                    if (a.name.matches(EMAIL_REGEXP)) /*Last resort, just look for an email address...*/ {
                        accountName = a.name;
                        accountType = a.type;
                        break;
                    }
                }
            }
        }

        String id = getJsonString(contact, "id");
        if (id != null) {
            if (getContactById(id) == null) {
                return null;
            }

            // Modify existing contact
            return modifyContact(id, contact, accountType, accountName);
        }

        // Create new contact
        return newContact(contact, accountType, accountName);
    }

    @Nullable
    private String newContact(JSONObject contact, String accountType, String accountName) {
        // Create a list of attributes to add to the contact database
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        //Add contact type
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build());

        // Add name
        String displayName = getJsonString(contact, "displayName");
        String firstName = getJsonString(contact, "firstName");
        String middleName = getJsonString(contact, "middleName");
        String familyName = getJsonString(contact, "familyName");
        if (displayName != null || firstName != null || middleName != null || familyName != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .build());
        } else {
            LOG.d(LOG_TAG, "All \"name\" properties are empty");
        }

        // Add organizationName
        String organizationName = getJsonString(contact, "organizationName");
        if(organizationName != null){
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organizationName)
                    .build());
        }

        //Add phone numbers
        JSONArray phones;
        try {
            phones = contact.getJSONArray("phoneNumbers");
            for (int i = 0; i < phones.length(); i++) {
                if (!phones.isNull(i)) {
                    JSONObject phone = (JSONObject) phones.get(i);
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, getJsonString(phone, "value"))
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, getPhoneType(getJsonString(phone, "type")))
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getJsonString(phone, "type"))
                            .build());
                }
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get phone numbers");
        }

        // Add emails
        JSONArray emails;
        try {
            emails = contact.getJSONArray("emails");
            for (int i = 0; i < emails.length(); i++) {
                JSONObject email = (JSONObject) emails.get(i);
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, getJsonString(email, "value"))
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, getMailType(getJsonString(email, "type")))
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, getJsonString(email, "type"))
                        .build());
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get emails");
        }

        // Add addresses
        JSONArray addresses;
        try {
            addresses = contact.getJSONArray("addresses");
            for (int i = 0; i < addresses.length(); i++) {
                JSONObject address = (JSONObject) addresses.get(i);
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, getJsonString(address, "streetAddress"))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, getJsonString(address, "locality"))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, getJsonString(address, "region"))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, getJsonString(address, "postalCode"))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, getJsonString(address, "country"))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, getAddressType(getJsonString(address, "type")))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, getJsonString(address, "type"))
                        .build());
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get addresses");
        }

        String newId = null;
        //Add contact
        try {
            ContentProviderResult[] cpResults = this.cordova.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if (cpResults.length > 0) {
                newId = cpResults[0].uri.getLastPathSegment();
            }
        } catch (RemoteException | OperationApplicationException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return newId;
    }

    @Nullable
    private String modifyContact(String id, JSONObject contact, String accountType, String accountName) {
        // Create a list of attributes to add to the contact database
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        //Add contact type
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build());

        // Modify name
        String displayName = getJsonString(contact, "displayName");
        String firstName = getJsonString(contact, "firstName");
        String middleName = getJsonString(contact, "middleName");
        String familyName = getJsonString(contact, "familyName");
        if (displayName != null || firstName != null || middleName != null || familyName != null) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                    ContactsContract.Data.MIMETYPE + "=?",
                            new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});

            if (displayName != null) {
                builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName);
            }
            if (familyName != null) {
                builder.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName);
            }
            if (middleName != null) {
                builder.withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName);
            }
            if (firstName != null) {
                builder.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName);
            }

            ops.add(builder.build());
        }

        // Modify organizationName
        String organizationName = getJsonString(contact, "organizationName");
        if(organizationName != null){
            try {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, id)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organizationName)
                        .build());
            } catch (Error error) {
                LOG.d(LOG_TAG, "Could not set organizationName" + error);
            }
        }

        // Modify phone numbers
        JSONArray phones;
        try {
            phones = contact.getJSONArray("phoneNumbers");
            // Delete all the phones
            if (phones.length() == 0) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                        ContactsContract.Data.MIMETYPE + "=?",
                                new String[]{id, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                        .build());
            }
            // Modify or add a phone
            else {
                for (int i = 0; i < phones.length(); i++) {
                    JSONObject phone = (JSONObject) phones.get(i);
                    String phoneId = getJsonString(phone, "id");
                    // This is a new phone so do a DB insert
                    if (phoneId == null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, id);
                        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, getJsonString(phone, "value"));
                        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, getPhoneType(getJsonString(phone, "type")));
                        contentValues.put(ContactsContract.CommonDataKinds.Phone.LABEL, getJsonString(phone, "type"));

                        ops.add(ContentProviderOperation.newInsert(
                                ContactsContract.Data.CONTENT_URI).withValues(contentValues).build());
                    }
                    // This is an existing phone so do a DB update
                    else {
                        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(ContactsContract.CommonDataKinds.Phone._ID + "=? AND " +
                                                ContactsContract.Data.MIMETYPE + "=?",
                                        new String[]{phoneId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, getJsonString(phone, "value"))
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, getPhoneType(getJsonString(phone, "type")))
                                .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getJsonString(phone, "type"))
                                .build());
                    }
                }
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get phone numbers");
        }

        // Modify emails
        JSONArray emails;
        try {
            emails = contact.getJSONArray("emails");
            // Delete all the emails
            if (emails.length() == 0) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                        ContactsContract.Data.MIMETYPE + "=?",
                                new String[]{id, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                        .build());
            }
            // Modify or add a email
            else {
                for (int i = 0; i < emails.length(); i++) {
                    JSONObject email = (JSONObject) emails.get(i);
                    String emailId = getJsonString(email, "id");
                    // This is a new email so do a DB insert
                    if (emailId == null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, id);
                        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                        contentValues.put(ContactsContract.CommonDataKinds.Email.DATA, getJsonString(email, "value"));
                        contentValues.put(ContactsContract.CommonDataKinds.Email.TYPE, getMailType(getJsonString(email, "type")));
                        contentValues.put(ContactsContract.CommonDataKinds.Email.LABEL, getJsonString(email, "type"));

                        ops.add(ContentProviderOperation.newInsert(
                                ContactsContract.Data.CONTENT_URI).withValues(contentValues).build());
                    }
                    // This is an existing email so do a DB update
                    else {
                        String emailValue = getJsonString(email, "value");
                        if (emailValue != null) {
                            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(ContactsContract.CommonDataKinds.Email._ID + "=? AND " +
                                                    ContactsContract.Data.MIMETYPE + "=?",
                                            new String[]{emailId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, getJsonString(email, "value"))
                                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, getMailType(getJsonString(email, "type")))
                                    .withValue(ContactsContract.CommonDataKinds.Email.LABEL, getJsonString(email, "type"))
                                    .build());
                        } else {
                            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(ContactsContract.CommonDataKinds.Email._ID + "=? AND " +
                                                    ContactsContract.Data.MIMETYPE + "=?",
                                            new String[]{emailId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                                    .build());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get emails");
        }

        // Modify addresses
        JSONArray addresses;
        try {
            addresses = contact.getJSONArray("addresses");
            // Delete all the addresses
            if (addresses.length() == 0) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                        ContactsContract.Data.MIMETYPE + "=?",
                                new String[]{id, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE})
                        .build());
            }
            // Modify or add a address
            else {
                for (int i = 0; i < addresses.length(); i++) {
                    JSONObject address = (JSONObject) addresses.get(i);
                    String addressId = getJsonString(address, "id");
                    // This is a new email so do a DB insert
                    if (addressId == null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, id);
                        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, getAddressType(getJsonString(address, "type")));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, getJsonString(address, "type"));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, getJsonString(address, "streetAddress"));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, getJsonString(address, "locality"));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, getJsonString(address, "region"));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, getJsonString(address, "postalCode"));
                        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, getJsonString(address, "country"));

                        ops.add(ContentProviderOperation.newInsert(
                                ContactsContract.Data.CONTENT_URI).withValues(contentValues).build());
                    }
                    // This is an existing address so do a DB update
                    else {
                        if (getJsonString(address, "streetAddress") != null
                                || getJsonString(address, "locality") != null
                                || getJsonString(address, "region") != null
                                || getJsonString(address, "postalCode") != null
                                || getJsonString(address, "country") != null) {
                            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(ContactsContract.CommonDataKinds.StructuredPostal._ID + "=? AND " +
                                                    ContactsContract.Data.MIMETYPE + "=?",
                                            new String[]{addressId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE})
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, getAddressType(getJsonString(address, "type")))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, getJsonString(address, "type"))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, getJsonString(address, "streetAddress"))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, getJsonString(address, "locality"))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, getJsonString(address, "region"))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, getJsonString(address, "postalCode"))
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, getJsonString(address, "country"))
                                    .build());
                        } else {
                            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(ContactsContract.CommonDataKinds.StructuredPostal._ID + "=? AND " +
                                                    ContactsContract.Data.MIMETYPE + "=?",
                                            new String[]{addressId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE})
                                    .build());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get addresses");
        }

        boolean retVal = true;
        //Modify contact
        try {
            this.cordova.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
            retVal = false;
        }

        // if the save was a success return the contact ID
        if (retVal) {
            return id;
        } else {
            return null;
        }
    }

    private void delete(JSONArray args) throws JSONException {
        final String contactId = args.getString(0);
        this.cordova.getThreadPool().execute(() -> {
            if (performDelete(contactId)) {
                _callbackContext.success();
            } else {
                returnError(ContactsXErrorCodes.UnknownError);
            }
        });
    }

    private boolean performDelete(String id) {
        int result = 0;

        try(Cursor cursor = this.cordova.getActivity().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts._ID + " = ?",
                new String[] { id },
                null)) {
            if (cursor == null || cursor.getCount() != 1) {
                LOG.d(LOG_TAG, "Could not find contact with ID");
                return false;
            }

            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);

            if (columnIndex >= 0) {
                String lookupKey = cursor.getString(columnIndex);
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                result = this.cordova.getActivity().getContentResolver().delete(uri, null, null);
            }

            return result > 0;
        }
    }

    private void hasPermission() throws JSONException {
        JSONObject response = new JSONObject();
        response.put("read", PermissionHelper.hasPermission(this, READ));
        response.put("write", PermissionHelper.hasPermission(this, WRITE));
        if (this._callbackContext != null) {
            this._callbackContext.success(response);
        }
    }

    private void requestPermission(boolean write) {
        PermissionHelper.requestPermission(this, REQ_CODE_PERMISSIONS, write ? WRITE : READ);
    }

    private void returnError(ContactsXErrorCodes errorCode) {
        returnError(errorCode, null);
    }

    private void returnError(ContactsXErrorCodes errorCode, String message) {
        if (_callbackContext != null) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("code", errorCode.value);
            resultMap.put("message", message == null ? "" : message);
            _callbackContext.error(new JSONObject(resultMap));
            _callbackContext = null;
        }
    }

    // Helper
    @Nullable
    private String getJsonString(JSONObject obj, String property) {
        String value = null;
        try {
            if (obj != null) {
                value = obj.getString(property);
            }
        } catch (JSONException e) {
            LOG.d(LOG_TAG, "Could not get = " + e.getMessage());
        }

        if (value != null) {
            return value.isEmpty() ? null : value;
        }

        return null;
    }

    /**
     * Converts a string from the W3C Contact API to it's Android int value.
     */
    private int getMailType(String string) {
        int type = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
        if (string != null) {

            String lowerType = string.toLowerCase(Locale.getDefault());

            switch (lowerType) {
                case "home":
                    return ContactsContract.CommonDataKinds.Email.TYPE_HOME;
                case "work":
                    return ContactsContract.CommonDataKinds.Email.TYPE_WORK;
                case "other":
                    return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
                case "mobile":
                    return ContactsContract.CommonDataKinds.Email.TYPE_MOBILE;
            }
        }
        return type;
    }

    /**
     * getPhoneType converts an Android mail type into a string
     */
    private String getMailType(int type) {
        String stringType;
        switch (type) {
            case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                stringType = "home";
                break;
            case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                stringType = "work";
                break;
            case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
                stringType = "mobile";
                break;
            case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
            default:
                stringType = "other";
                break;
        }
        return stringType;
    }

    /**
     * Converts a string from the W3C Contact API to it's Android int value.
     */
    private int getAddressType(String string) {
        int type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;
        if (string != null) {
            String lowerType = string.toLowerCase(Locale.getDefault());

            switch (lowerType) {
                case "home":
                    return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;
                case "work":
                    return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;
                case "other":
                    return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;
            }
        }
        return type;
    }

    /**
     * getPhoneType converts an Android mail type into a string
     */
    private String getAddressType(int type) {
        String stringType;
        switch (type) {
            case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
                stringType = "home";
                break;
            case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
                stringType = "work";
                break;
            case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER:
            default:
                stringType = "other";
                break;
        }
        return stringType;
    }

    /**
     * getPhoneType converts an Android phone type into a string
     *
     * @return phone type as string.
     */
    private String getPhoneType(int type) {
        String stringType;

        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                stringType = "home";
                break;
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                stringType = "work";
                break;
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                stringType = "mobile";
                break;
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
            default:
                stringType = "other";
                break;
        }
        return stringType;
    }

    /**
     * Converts a string from the W3C Contact API to it's Android int value.
     *
     * @return Android int value
     */
    private int getPhoneType(String string) {

        int type = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;

        if (string != null) {
            String lowerType = string.toLowerCase(Locale.getDefault());

            switch (lowerType) {
                case "home":
                    return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                case "work":
                    return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
                case "mobile":
                    return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            }
        }
        return type;
    }

    private Contacts getContactsFactory() {
        if (contactsFactory == null) {
            contactsFactory = ContactsFactory
                    .create(cordova.getContext());
        }

        return contactsFactory;
    }
}
