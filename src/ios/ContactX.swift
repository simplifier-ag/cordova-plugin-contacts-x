import Contacts

class ContactX {

    var contact: CNContact;
    var options: ContactsXOptions;
    
    init(contact: CNContact, options: ContactsXOptions) {
        self.contact = contact;
        self.options = options;
    }

    func getEmailAddresses() -> [NSDictionary] {
        let labeledValues: [NSDictionary] = self.contact.emailAddresses.map { (ob: CNLabeledValue<NSString>) -> NSDictionary in
            return [
                "id": ob.identifier,
                "type": ContactsX.mapLabelToString(label: ob.label ?? ""),
                "value": ob.value
            ]
        }
        return labeledValues;
    }

    func getPhoneNumbers() -> [NSDictionary] {
        let labeledValues: [NSDictionary] = self.contact.phoneNumbers.map { (ob: CNLabeledValue<CNPhoneNumber>) -> NSDictionary in
            return [
                "id": ob.identifier,
                "type": ContactsX.mapLabelToString(label: ob.label ?? ""),
                "normalized" : self.getNormalizedPhoneNumber(phoneNumberString: ob.value.stringValue),
                "value": ob.value.stringValue
            ]
        }
        return labeledValues;
    }
    
    func getAddresses() -> [NSDictionary] {
        let labeledValues: [NSDictionary] = self.contact.postalAddresses.map { (ob: CNLabeledValue<CNPostalAddress>) -> NSDictionary in
            return [
                "id": ob.identifier,
                "type": ContactsX.mapLabelToString(label: ob.label ?? ""),
                "streetAddress": ob.value.street,
                "locality": ob.value.city,
                "region": ob.value.state,
                "postalCode": ob.value.postalCode,
                "country": ob.value.country
            ]
        }
        return labeledValues;
    }

    func getJson() -> NSDictionary {

        var phoneNumbers: [NSDictionary] = [];
        if(options.phoneNumbers) {
            phoneNumbers = self.getPhoneNumbers();
        }

        var emails: [NSDictionary] = [];
        if(options.emails) {
            emails = self.getEmailAddresses();
        }
        
        var addresses: [NSDictionary] = [];
        if(options.addresses) {
            addresses = self.getAddresses();
        }

        var result: [String : Any] = [
            "id": self.contact.identifier,
            "phoneNumbers": phoneNumbers,
            "emails": emails,
            "addresses": addresses
        ];

        if(options.firstName) {
            result["firstName"] = self.contact.givenName;
        }
        if(options.middleName) {
            result["middleName"] = self.contact.middleName;
        }
        if(options.familyName) {
            result["familyName"] = self.contact.familyName;
        }
        if(options.organizationName) {
            result["organizationName"] = self.contact.organizationName;
        }

        return result as NSDictionary;
    }
    
    private func getNormalizedPhoneNumber(phoneNumberString: String) -> String {
		guard phoneNumberString.isEmpty, let baseCountryCode = options.baseCountryCode else {
			return ""
		}
		
		do {
			let phoneNumberCustomDefaultRegion = try ContactsX
				.getPhoneNumberKitInstance()
				.parse(phoneNumberString, withRegion: baseCountryCode, ignoreType: true)
			
			return ContactsX.getPhoneNumberKitInstance().format(phoneNumberCustomDefaultRegion, toType : .e164)
		}
		catch {
			return "";
		}
    }
}
