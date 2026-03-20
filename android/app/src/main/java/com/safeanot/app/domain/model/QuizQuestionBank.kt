/**
 * Hardcoded question bank for the "Spot the Scam" quiz feature.
 * Contains 15+ questions across 5 categories: phishing URLs, social engineering,
 * fake apps, payment scams, and OTP theft.
 */
package com.safeanot.app.domain.model

object QuizQuestionBank {

    val questions: List<QuizQuestion> = listOf(
        // --- Phishing URLs ---
        QuizQuestion(
            id = "phish_01",
            scenario = "You receive an SMS: 'Your Maybank account has been locked. Click here to verify: maybank-secure-login.com'. What should you do?",
            options = listOf(
                "Click the link and enter your login details",
                "Ignore the message and check via the official Maybank app",
                "Forward the message to your friends to warn them",
                "Reply to the SMS asking for more details",
            ),
            correctIndex = 1,
            explanation = "The URL 'maybank-secure-login.com' is not an official Maybank domain. Always access your bank through the official app or website (maybank2u.com.my). Legitimate banks never ask you to verify accounts via SMS links.",
        ),
        QuizQuestion(
            id = "phish_02",
            scenario = "You see an email from 'service@paypa1.com' asking you to confirm a payment. The '1' replaces the letter 'l'. Is this legitimate?",
            options = listOf(
                "Yes, it's just a font issue",
                "No, this is a phishing attempt using a lookalike domain",
                "Yes, PayPal sometimes uses different domains",
            ),
            correctIndex = 1,
            explanation = "Scammers often use lookalike domains by substituting similar-looking characters (like '1' for 'l'). Always check the sender's email address carefully. The real PayPal domain is paypal.com.",
        ),
        QuizQuestion(
            id = "phish_03",
            scenario = "A WhatsApp message says: 'Congratulations! You won RM10,000 from Shopee. Claim here: bit.ly/shopee-prize'. What do you do?",
            options = listOf(
                "Click quickly before the prize expires",
                "Check if you entered any Shopee contest recently",
                "Ignore it -- legitimate companies don't announce prizes via random WhatsApp messages with shortened URLs",
                "Share with family so they can claim too",
            ),
            correctIndex = 2,
            explanation = "Legitimate prize notifications come through official app notifications, not random WhatsApp messages. Shortened URLs (bit.ly) hide the real destination and are commonly used in scams. Never click links in unsolicited prize messages.",
        ),

        // --- Social Engineering ---
        QuizQuestion(
            id = "social_01",
            scenario = "Someone calls claiming to be from LHDN (tax authority) saying you owe back taxes and will be arrested if you don't pay immediately via bank transfer. What should you do?",
            options = listOf(
                "Transfer the money immediately to avoid arrest",
                "Ask for their badge number and call LHDN's official number to verify",
                "Give them your IC number so they can look up your records",
                "Pay half now and half later",
            ),
            correctIndex = 1,
            explanation = "Government agencies never threaten arrest over the phone or demand immediate payment via bank transfer. This is a common impersonation scam. Always hang up and call the official number to verify any claims.",
        ),
        QuizQuestion(
            id = "social_02",
            scenario = "A 'police officer' calls saying your bank account is linked to money laundering. They ask you to transfer funds to a 'safe account' for investigation. What do you do?",
            options = listOf(
                "Cooperate and transfer the money to help the investigation",
                "Refuse -- police never ask you to transfer money to 'safe accounts' over the phone",
                "Ask them to send an official letter first",
                "Give them your account details so they can check",
            ),
            correctIndex = 1,
            explanation = "Police never ask civilians to transfer money to 'safe accounts' over the phone. This is a classic Macau scam. Real investigations involve official letters and in-person visits. Hang up and report to the police.",
        ),
        QuizQuestion(
            id = "social_03",
            scenario = "Your friend messages you on Facebook asking to borrow RM500 urgently because they're stuck overseas. Their profile looks normal. What should you do?",
            options = listOf(
                "Transfer the money right away since they're your friend",
                "Call or video-call your friend directly to verify it's really them",
                "Send the money but ask for it back later",
                "Ask them for their bank account details via the same chat",
            ),
            correctIndex = 1,
            explanation = "Social media accounts can be hacked or cloned. Always verify urgent money requests through a different channel -- call them directly or video-call. Scammers copy profiles to trick friends into sending money.",
        ),

        // --- Fake Apps ---
        QuizQuestion(
            id = "fakeapp_01",
            scenario = "A friend shares a WhatsApp link to download a 'special trading app' that guarantees 30% monthly returns. Is this safe?",
            options = listOf(
                "Yes, if my friend recommends it",
                "No -- apps shared via WhatsApp links (not from Play Store) and promising guaranteed high returns are almost certainly scams",
                "Yes, high returns are normal in trading",
                "It depends on the app's reviews",
            ),
            correctIndex = 1,
            explanation = "Legitimate apps are distributed through official app stores (Google Play, App Store), not WhatsApp links. 'Guaranteed' high returns are a hallmark of investment scams. Your friend may also be a victim without knowing.",
        ),
        QuizQuestion(
            id = "fakeapp_02",
            scenario = "You find a 'Free Netflix Premium' APK file on a website. It asks for permissions to access SMS and contacts. Should you install it?",
            options = listOf(
                "Yes, free Netflix sounds great",
                "No -- unofficial APKs requesting SMS/contact access are likely malware that can steal your OTPs and personal data",
                "Yes, but only if the website looks professional",
                "Yes, after scanning it with antivirus",
            ),
            correctIndex = 1,
            explanation = "APK files from unofficial sources often contain malware. An app claiming to be Netflix would never need SMS or contacts permissions. These permissions allow the app to intercept OTPs and steal your contacts for further scams.",
        ),
        QuizQuestion(
            id = "fakeapp_03",
            scenario = "An app on the Play Store has 5 stars but only 12 reviews, all posted on the same day. Should you trust it?",
            options = listOf(
                "Yes, 5 stars means it's good",
                "Be cautious -- fake reviews posted on the same day are a red flag even on official stores",
                "Yes, it's on the Play Store so it must be safe",
            ),
            correctIndex = 1,
            explanation = "While the Play Store has protections, fake apps can still appear briefly. Red flags include: very few reviews, all reviews posted on the same day, generic praise, and the app being newly published. Check the developer's other apps and overall reputation.",
        ),

        // --- Payment Scams ---
        QuizQuestion(
            id = "payment_01",
            scenario = "An online seller asks you to pay via direct bank transfer instead of using Shopee/Lazada's built-in payment. They say it's cheaper. What should you do?",
            options = listOf(
                "Pay by bank transfer to save money",
                "Refuse -- always use the platform's official payment system which offers buyer protection",
                "Pay by transfer but screenshot everything",
                "Negotiate a lower price for paying directly",
            ),
            correctIndex = 1,
            explanation = "Platform payment systems (Shopee Pay, Lazada Wallet) offer buyer protection and dispute resolution. Direct bank transfers have no recourse if the seller scams you. Sellers who insist on off-platform payment are often fraudulent.",
        ),
        QuizQuestion(
            id = "payment_02",
            scenario = "You see a job posting on social media: 'Earn RM300/day from home! Just like and review products. Send RM50 registration fee to start.' Is this legitimate?",
            options = listOf(
                "Yes, RM50 is a small investment for RM300/day",
                "No -- legitimate jobs never require you to pay a registration fee upfront",
                "Maybe, if the company has a website",
                "Yes, many online jobs work this way",
            ),
            correctIndex = 1,
            explanation = "Job scams lure victims with high pay for easy work, then ask for upfront fees. Legitimate employers never charge employees to start working. This is a common task scam where victims pay fees and never receive the promised income.",
        ),
        QuizQuestion(
            id = "payment_03",
            scenario = "Someone contacts you to buy your item on Carousell but says their 'payment is pending' and sends a fake bank transfer screenshot. What do you do?",
            options = listOf(
                "Ship the item since you can see the transfer screenshot",
                "Wait for the money to actually appear in your bank account before shipping",
                "Ask them to resend the payment",
                "Accept the screenshot as proof",
            ),
            correctIndex = 1,
            explanation = "Fake bank transfer screenshots are extremely easy to create. Never ship items or release goods until the money is confirmed in your account. Check your actual bank balance -- not screenshots or SMS notifications, which can also be faked.",
        ),

        // --- OTP Theft ---
        QuizQuestion(
            id = "otp_01",
            scenario = "You receive an OTP code you didn't request, then someone messages asking you to 'forward the code that was sent to your number by mistake'. What do you do?",
            options = listOf(
                "Forward the code since it was sent by mistake",
                "Never share OTP codes -- someone is trying to access your account using your phone number",
                "Forward it only if they seem trustworthy",
                "Ask them what the code is for first",
            ),
            correctIndex = 1,
            explanation = "OTPs are one-time passwords for YOUR accounts. If you receive one you didn't request, someone is trying to access your account. NEVER share OTP codes with anyone -- not even bank staff, family, or friends. This is a common account takeover technique.",
        ),
        QuizQuestion(
            id = "otp_02",
            scenario = "A caller claiming to be from your bank says they need your TAC (OTP) code to 'block a suspicious transaction' on your account. Should you share it?",
            options = listOf(
                "Yes, they're trying to protect your account",
                "No -- banks never ask for your OTP/TAC codes over the phone",
                "Yes, but only the last 3 digits",
                "Ask them to verify your IC number first",
            ),
            correctIndex = 1,
            explanation = "Banks NEVER call to ask for OTP/TAC codes. These codes authorize transactions -- sharing them allows scammers to transfer your money. If concerned about your account, hang up and call your bank's official hotline directly.",
        ),
        QuizQuestion(
            id = "otp_03",
            scenario = "You receive an email saying 'Your WhatsApp verification code is 482910. Do not share this code.' Then a friend asks for it on WhatsApp. What's happening?",
            options = listOf(
                "Your friend needs help setting up their WhatsApp",
                "Someone is trying to hijack your WhatsApp account -- the 'friend' account may already be compromised",
                "WhatsApp sent the code by mistake",
                "It's safe to share since it's just WhatsApp, not a bank",
            ),
            correctIndex = 1,
            explanation = "This is a WhatsApp account hijacking attempt. The scammer triggers a verification code to your number, then asks for it (often from an already-hijacked friend's account). Sharing the code gives them full access to your WhatsApp, which they'll use to scam your contacts.",
        ),
    )

    /**
     * Returns [count] randomly selected and shuffled questions.
     * If [count] exceeds the total number of questions, all questions are returned shuffled.
     */
    fun getRandomQuiz(count: Int = 5): List<QuizQuestion> {
        return questions.shuffled().take(count)
    }
}
