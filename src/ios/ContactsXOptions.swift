class ContactsXOptions {

    var firstName: Bool = true;
    var middleName: Bool = true;
    var familyName: Bool = true;
    var organizationName: Bool = true;
    var phoneNumbers: Bool = false;
    var emails: Bool = false;
    var addresses: Bool = false;
    var baseCountryCode : String?? = nil;

    init(options: NSDictionary?) {
        if(options != nil) {

            let fields = options?.value(forKey: "fields") as? NSDictionary ?? nil;
            if(fields != nil) {
                self.parseFields(fields: fields!)
            }

            let baseCountryCode = options?.value(forKey: "baseCountryCode") as? String ?? nil;
            
            if(baseCountryCode != nil) {
                self.baseCountryCode = baseCountryCode;
            }
        }
    }

    private func parseFields(fields: NSDictionary) {
        firstName = fields.value(forKey: "firstName") as? Bool ?? true;
        middleName = fields.value(forKey: "middleName") as? Bool ?? true;
        familyName = fields.value(forKey: "familyName") as? Bool ?? true;
        organizationName = fields.value(forKey: "organizationName") as? Bool ?? true;
        phoneNumbers = fields.value(forKey: "phoneNumbers") as? Bool ?? false;
        emails = fields.value(forKey: "emails") as? Bool ?? false;
        addresses = fields.value(forKey: "addresses") as? Bool ?? false;
    }

}

class ContactXOptions {
    var id: String? = nil;
    var firstName: String? = nil;
    var middleName: String? = nil;
    var familyName: String? = nil;
    var organizationName: String? = nil;
    var phoneNumbers: [ContactXValueTypeOptions]? = nil;
    var emails: [ContactXValueTypeOptions]? = nil;
    var addresses: [ContactXAddressOptions]? = nil
    
    init(options: NSDictionary?) {
        if(options != nil) {
            id = options?.value(forKey: "id") as? String;
            firstName = options?.value(forKey: "firstName") as? String;
            middleName = options?.value(forKey: "middleName") as? String;
            familyName = options?.value(forKey: "familyName") as? String;
            organizationName = options?.value(forKey: "organizationName") as? String;
            let phonenumberArray = options?.value(forKey: "phoneNumbers") as? [NSDictionary];
            if(phonenumberArray != nil) {
                phoneNumbers = self.parsePhoneNumbers(array: phonenumberArray!);
            }
            let emailsArray = options?.value(forKey: "emails") as? [NSDictionary];
            if(emailsArray != nil) {
                emails = self.parseEmails(array: emailsArray!);
            }
            let addressArray = options?.value(forKey: "addresses") as? [NSDictionary];
            if(addressArray != nil) {
                addresses = self.parseAddresses(array: addressArray!);
            }
        }
    }
    
    private func parsePhoneNumbers(array: [NSDictionary]) -> [ContactXValueTypeOptions] {
        var numbers: [ContactXValueTypeOptions] = [];
        for numberObject in array {
            let finalNumber = ContactXValueTypeOptions.init(options: numberObject);
            if(finalNumber.type != "" && finalNumber.value != "") {
                numbers.append(finalNumber);
            }
        }
        return numbers;
    }
    
    private func parseEmails(array: [NSDictionary]) -> [ContactXValueTypeOptions] {
        var mails: [ContactXValueTypeOptions] = [];
        for mailObject in array {
            let finalMail = ContactXValueTypeOptions.init(options: mailObject);
            if(finalMail.type != "" && finalMail.value != "") {
                mails.append(finalMail);
            }
        }
        return mails;
    }
    
    private func parseAddresses(array: [NSDictionary]) -> [ContactXAddressOptions] {
        var addresses: [ContactXAddressOptions] = [];
        for addressObject in array {
            let finalAddress = ContactXAddressOptions.init(options: addressObject);
            if(finalAddress.type != ""
               && (finalAddress.streetAddress != ""
                   || finalAddress.locality != ""
                   || finalAddress.region != ""
                   || finalAddress.postalCode != ""
                   || finalAddress.country != "")) {
                addresses.append(finalAddress);
            }
        }
        return addresses;
    }
}

class ContactXValueTypeOptions {
    var id: String? = nil;
    var type: String;
    var value: String;
    
    init(options: NSDictionary) {
        id = options.value(forKey: "id") as? String;
        type = options.value(forKey: "type") as? String ?? "";
        value = options.value(forKey: "value") as? String ?? "";
    }
}

class ContactXAddressOptions {
    var id: String? = nil;
    var type: String;
    var streetAddress: String;
    var locality: String;
    var region: String;
    var postalCode: String;
    var country: String;
    
    init(options: NSDictionary) {
        id = options.value(forKey: "id") as? String;
        type = options.value(forKey: "type") as? String ?? "";
        streetAddress = options.value(forKey: "streetAddress") as? String ?? "";
        locality = options.value(forKey: "locality") as? String ?? "";
        region = options.value(forKey: "region") as? String ?? "";
        postalCode = options.value(forKey: "postalCode") as? String ?? "";
        country = options.value(forKey: "country") as? String ?? "";
    }
}
