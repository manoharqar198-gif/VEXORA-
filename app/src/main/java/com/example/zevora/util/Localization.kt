package com.example.zevora.util

import com.example.zevora.model.AppLanguage

object ZevoraStrings {
  fun get(key: String, language: AppLanguage): String {
    val isHi = language == AppLanguage.HINDI
    val isMr = language == AppLanguage.MARATHI
    val isBn = language == AppLanguage.BENGALI

    return when (key) {
      // Navigation
      "nav_home" -> when {
        isHi -> "होम"
        isMr -> "मुख्यपृष्ठ"
        isBn -> "হোম"
        else -> "Home"
      }
      "nav_reels" -> when {
        isHi -> "रील्स"
        isMr -> "रील्स"
        isBn -> "রিলস"
        else -> "Reels"
      }
      "nav_watch" -> when {
        isHi -> "वीडियो"
        isMr -> "व्हिडिओ"
        isBn -> "ভিডিও"
        else -> "Watch"
      }
      "nav_chat" -> when {
        isHi -> "चैट"
        isMr -> "गप्पा"
        isBn -> "চ্যাট"
        else -> "Chat"
      }
      "nav_profile" -> when {
        isHi -> "प्रोफाइल"
        isMr -> "प्रोफाइल"
        isBn -> "প্রোফাইল"
        else -> "Profile"
      }
      "nav_security" -> when {
        isHi -> "सुरक्षा और गोपनीयता"
        isMr -> "सुरक्षा आणि गोपनीयता"
        isBn -> "নিরাপত্তা ও গোপনীয়তা"
        else -> "Security & Privacy"
      }
      "nav_settings" -> when {
        isHi -> "सेटिंग्स"
        isMr -> "सेटिंग्ज"
        isBn -> "সেটিংস"
        else -> "Settings"
      }
      "creator_studio" -> when {
        isHi -> "कमाई का सिस्टम (Creator Studio)"
        isMr -> "कमाई केंद्र"
        isBn -> "উপার্জন কেন্দ্র"
        else -> "Earnings & Creator Studio"
      }

      // Security Section
      "sec_title" -> when {
        isHi -> "सुरक्षा और गोपनीयता (Security & Privacy)"
        isMr -> "सुरक्षा आणि गोपनीयता"
        isBn -> "নিরাপত্তা ও গোপনীয়তা"
        else -> "Security & Privacy"
      }
      "sec_otp_verify" -> when {
        isHi -> "OTP Verification (असली ओटीपी जांच)"
        isMr -> "OTP पडताळणी"
        isBn -> "ওটিপি যাচাইকরণ"
        else -> "OTP Verification"
      }
      "sec_safe_pwd" -> when {
        isHi -> "सुरक्षित Password (पासवर्ड स्ट्रेंथ मीटर)"
        isMr -> "सुरक्षित पासवर्ड"
        isBn -> "সুরক্ষিত পাসওয়ার্ড"
        else -> "Secure Password Checker"
      }
      "sec_2fa" -> when {
        isHi -> "Two-Factor Authentication (2FA दो-चरणीय सुरक्षा)"
        isMr -> "दोन-घटक प्रमाणीकरण"
        isBn -> "দ্বি-পদক্ষেপ যাচাইকরণ"
        else -> "Two-Factor Authentication (2FA)"
      }
      "sec_private_acc" -> when {
        isHi -> "Private Account (निजी खाता)"
        isMr -> "खाजगी खाते"
        isBn -> "ব্যক্তিগত অ্যাকাউন্ট"
        else -> "Private Account"
      }
      "sec_private_desc" -> when {
        isHi -> "केवल स्वीकृत फॉलोअर्स ही आपकी पोस्ट और रील्स देख सकेंगे।"
        isMr -> "केवळ मंजूर फॉलोअर्स तुमच्या पोस्ट पाहू शकतील."
        isBn -> "শুধুমাত্র অনুমোদিত ফলোয়াররাই আপনার পোস্ট দেখতে পাবেন।"
        else -> "Only approved followers can view your posts and reels."
      }
      "sec_block_user" -> when {
        isHi -> "Block User (ब्लॉक किए गए यूजर्स)"
        isMr -> "ब्लॉक केलेले वापरकर्ते"
        isBn -> "ব্লক করা ব্যবহারকারী"
        else -> "Block / Blocked Users"
      }
      "sec_report_user" -> when {
        isHi -> "Report User (यूजर की रिपोर्ट करें)"
        isMr -> "वापरकर्त्याची तक्रार करा"
        isBn -> "ব্যবহারকারী রিপোর্ট করুন"
        else -> "Report User"
      }
      "sec_report_post" -> when {
        isHi -> "Report Post (पोस्ट की रिपोर्ट करें)"
        isMr -> "पोस्टची तक्रार करा"
        isBn -> "পোস্ট রিপোর্ট করুন"
        else -> "Report Post"
      }
      "sec_comment_ctrl" -> when {
        isHi -> "Comment Control (टिप्पणी नियंत्रण)"
        isMr -> "कमेंट नियंत्रण"
        isBn -> "মন্তব্য নিয়ন্ত্রণ"
        else -> "Comment Controls"
      }
      "sec_login_activity" -> when {
        isHi -> "Login Activity (सक्रिय डिवाइसेस)"
        isMr -> "लॉगिन गतिविधी"
        isBn -> "লগইন অ্যাক্টিভিটি"
        else -> "Login Activity & Devices"
      }
      "sec_suspicious_alert" -> when {
        isHi -> "Suspicious Login Alert (संदिग्ध लॉगिन चेतावनी)"
        isMr -> "संशयास्पद लॉगिन सूचना"
        isBn -> "সন্দেহজনক লগইন সতর্কতা"
        else -> "Suspicious Login Alerts"
      }

      // Settings Section
      "set_title" -> when {
        isHi -> "सेटिंग्स (Settings)"
        isMr -> "सेटिंग्ज"
        isBn -> "সেটিংস"
        else -> "Settings"
      }
      "set_lang" -> when {
        isHi -> "Language बदलना (भाषा चुनें)"
        isMr -> "भाषा बदला"
        isBn -> "ভাষা পরিবর্তন করুন"
        else -> "Change Language"
      }
      "set_theme" -> when {
        isHi -> "Dark Mode / Light Mode"
        isMr -> "डार्क / लाइट मोड"
        isBn -> "ডার্ক / লাইট মোড"
        else -> "Theme (Dark / Light Mode)"
      }
      "set_notifications" -> when {
        isHi -> "Notification Settings (सूचना सेटिंग्स)"
        isMr -> "सूचना सेटिंग्ज"
        isBn -> "বিজ্ঞপ্তি সেটিংস"
        else -> "Notification Settings"
      }
      "set_privacy" -> when {
        isHi -> "Privacy Settings (गोपनीयता सेटिंग्स)"
        isMr -> "गोपनीयता सेटिंग्ज"
        isBn -> "গোপনীয়তা সেটিংস"
        else -> "Privacy Settings"
      }
      "set_change_pwd" -> when {
        isHi -> "Password बदलना (पासवर्ड बदलें)"
        isMr -> "पासवर्ड बदला"
        isBn -> "পাসওয়ার্ড পরিবর্তন করুন"
        else -> "Change Password"
      }
      "set_del_acc" -> when {
        isHi -> "Account Delete करना (खाता हटाएं)"
        isMr -> "खाते हटवा"
        isBn -> "অ্যাকাউন্ট মুছুন"
        else -> "Delete Account"
      }
      "set_help" -> when {
        isHi -> "Help और Support (सहायता केंद्र)"
        isMr -> "मदत आणि समर्थन"
        isBn -> "সাহায্য ও সহায়তা"
        else -> "Help & Support"
      }
      "set_terms" -> when {
        isHi -> "Terms & Conditions (नियम व शर्तें)"
        isMr -> "नियम व अटी"
        isBn -> "শর্তাবলী"
        else -> "Terms & Conditions"
      }
      "set_privacy_policy" -> when {
        isHi -> "Privacy Policy (गोपनीयता नीति)"
        isMr -> "गोपनीयता धोरण"
        isBn -> "গোপনীয়তা নীতি"
        else -> "Privacy Policy"
      }
      "set_contact_us" -> when {
        isHi -> "Contact Us (संपर्क करें)"
        isMr -> "आमच्याशी संपर्क साधा"
        isBn -> "আমাদের সাথে যোগাযোগ করুন"
        else -> "Contact Us"
      }
      "set_logout" -> when {
        isHi -> "Log Out (लॉग आउट करें)"
        isMr -> "लॉग आउट"
        isBn -> "লগ আউট"
        else -> "Log Out"
      }

      // Default fallback
      else -> key
    }
  }
}
