package com.mhj.player.core.model

/**
 * Menu language. Every Persian label in the app goes through [t]; in English mode it is looked
 * up in [EN]. The app rebuilds its screens when the language changes, so a plain flag is enough.
 */
object L {
    @Volatile
    var en: Boolean = false

    fun t(fa: String): String = if (en) EN[fa] ?: fa else fa

    /** For sentences with values in them. */
    fun pick(fa: String, en: String): String = if (this.en) en else fa

    private val EN = mapOf(
        // General
        "حذف" to "Delete", "انصراف" to "Cancel", "ذخیره" to "Save", "تغییر نام" to "Rename", "نام جدید" to "New name",
        "نام تازه" to "New name", "بیشتر" to "More", "برگشت" to "Back", "ساختن" to "Create", "خاموش" to "Off",
        "اشتراک‌گذاری" to "Share", "حذف فایل" to "Delete file", "مطمئنی می‌خوای از دستگاه حذف بشه؟" to "Delete this file from the phone?",
        "این گوشی پنجرهٔ کوچک ندارد" to "This phone does not support picture-in-picture",
        "خوانندهٔ نامشخص" to "Unknown artist", "خودکار" to "Automatic", "پلی‌لیست" to "Playlist",
        // Top menu
        "کتابخانه" to "Library", "همهٔ آهنگ‌ها و ویدیوهای گوشی" to "All songs and videos on the phone",
        "پلی‌لیست‌ها" to "Playlists", "فهرست‌های خودتان و فهرست‌های خودکار" to "Your lists and automatic lists",
        "علاقه‌مندی‌ها" to "Favorites", "آهنگ‌هایی که قلب زده‌اید" to "Songs you have hearted",
        "ظاهر" to "Appearance", "رنگ‌بندی برنامه" to "App colours", "شب طلایی" to "Midnight Gold", "سفید چینی" to "Porcelain White",
        "زمینهٔ تیره با طلایی" to "Dark background with gold", "زمینهٔ روشن کرم با طلایی" to "Light cream background with gold",
        "زبان" to "Language", "زبان منوها" to "Menu language", "فارسی" to "فارسی", "انگلیسی" to "English",
        "منوها به فارسی، راست‌به‌چپ" to "Persian menus, right-to-left", "منوها به انگلیسی، چپ‌به‌راست" to "English menus, left-to-right",
        "افزودن فایل" to "Add file", "در حال جستجو…" to "Scanning…", "جستجوی گوشی" to "Scan phone", "افزودن پوشه" to "Add folder",
        "جستجوی نام، خواننده، آلبوم، پوشه…" to "Search name, artist, album, folder…",
        "همه" to "All", "موسیقی" to "Music", "ویدیو" to "Video", "پخش‌شده" to "Played", "پخش‌نشده" to "Unplayed",
        "حذف از دستگاه" to "Delete from phone", "افزودن به پلی‌لیست" to "Add to playlist",
        "برای ویرایش، حذف یا جابه‌جایی، روی پلی‌لیست انگشت نگه دارید" to "Hold a playlist to edit, delete or move it",
        "نام پلی‌لیست تازه…" to "New playlist name…", "نام پلی‌لیست تازه" to "New playlist name",
        "هنوز پلی‌لیستی نساخته‌اید؛ نام بنویسید و + را بزنید." to "No playlists yet — type a name and tap +.",
        "پلی‌لیست شما" to "Your playlist", "حذف پلی‌لیست" to "Delete playlist", "تغییر نام پلی‌لیست" to "Rename playlist",
        "پخش همه" to "Play all", "برای جابه‌جایی، روی آهنگ انگشت نگه دارید و بکشید" to "Hold a song and drag to move it",
        "این پلی‌لیست خالی است" to "This playlist is empty", "حذف از این پلی‌لیست" to "Remove from this playlist",
        "تازه‌ها" to "Newest", "مدتی پخش‌نشده" to "Not played lately", "پرپخش‌ترین" to "Most played",
        "نام" to "Name", "خواننده" to "Artist", "آلبوم" to "Album", "تاریخ افزودن" to "Date added", "مدت" to "Length",
        "تعداد پخش" to "Play count", "آخرین پخش" to "Last played", "پوشه" to "Folder", "قالب" to "Format",
        // Playlist picker
        "جستجوی پلی‌لیست…" to "Search playlists…", "پلی‌لیست تازه" to "New playlist", "از قبل اضافه شده" to "Already added",
        "پلی‌لیستی با این نام نیست" to "No playlist with that name",
        // Player
        "آهنگی پخش نمی‌شود" to "Nothing playing", "روی لوگو بزنید و یک آهنگ انتخاب کنید" to "Tap the logo and pick a song",
        "برداشتن از علاقه‌مندی‌ها" to "Remove from favorites", "افزودن به علاقه‌مندی‌ها" to "Add to favorites",
        "پخش درهم" to "Shuffle", "قبلی" to "Previous", "بعدی" to "Next", "تکرار خاموش" to "Repeat off",
        "تکرار یک آهنگ" to "Repeat one", "تکرار همه" to "Repeat all", "پردازش صدا" to "Sound", "توقف" to "Pause", "پخش" to "Play",
        // Sound panel
        "تنظیم‌ها روی همهٔ آهنگ‌ها و ویدیوها می‌مانند" to "Settings apply to every song and video",
        "وضوح گفتگو" to "Dialogue clarity", "صدای حرف‌زدن واضح‌تر، غرش بم کمتر" to "Clearer speech, less rumble",
        "حالت شب" to "Night mode", "صداهای بلند آرام‌تر، صداهای آرام شنیدنی‌تر" to "Loud parts softer, quiet parts clearer",
        "تکرار A-B" to "A-B repeat", "روشن — بزنید تا خاموش شود" to "On — tap to turn off", "نقطهٔ B را بزنید" to "Tap to set point B",
        "نقطهٔ A را بزنید" to "Tap to set point A", "سرعت" to "Speed", "اکولایزر" to "Equalizer", "تایمر خواب" to "Sleep timer",
        "سرعت پخش" to "Playback speed", "عقربه را بچرخانید" to "Turn the hand", "کمتر" to "Slower", "بیشتر " to "Faster",
        "حفظ صدای طبیعی" to "Keep natural pitch", "با تند یا کند شدن، صدا زیر یا بم نمی‌شود" to "Voices don't get higher or lower with speed",
        "بعد از تمام شدن همین آهنگ، پخش می‌ایستد" to "Playback stops when this song ends",
        "عقربه را بچرخانید یا یکی از گزینه‌ها را بزنید" to "Turn the hand or tap an option",
        "پایان" to "End", "پایان آهنگ" to "End of song", "دقیقه" to "min", "همین آهنگ" to "this song", "مانده" to "left",
        "پایان همین آهنگ" to "End of this song", "بلندی کلی" to "Overall level", "برگرداندن به صاف" to "Reset to flat", "صاف" to "Flat",
        "بالا و پایین بکشید" to "Drag up and down",
        // EQ presets
        "بیس" to "Bass", "بیس قوی" to "Strong bass", "تریبل" to "Treble", "صدای خواننده" to "Vocal", "پاپ" to "Pop", "راک" to "Rock",
        "کلاسیک" to "Classical", "جز" to "Jazz", "الکترونیک" to "Electronic", "سنتی" to "Persian traditional", "هدفون" to "Headphones",
        "بلندگوی گوشی" to "Phone speaker", "دلخواه" to "Custom",
        // Video
        "پنجرهٔ کوچک" to "Picture-in-picture", "باز کردن ویدیو" to "Open video", "فضای ویدیو" to "Video",
        "یک ویدیو انتخاب کنید یا از کتابخانه بزنید" to "Pick a video, or play one from the library",
        "۱۰ ثانیه عقب" to "Back 10 seconds", "۱۰ ثانیه جلو" to "Forward 10 seconds",
        // Backup, folders, bookmarks
        "انتقال پلی‌لیست‌ها به گوشی دیگر" to "Move your playlists to another phone",
        "این فایل فقط فهرست است؛ خود آهنگ‌ها را جدا به گوشی جدید منتقل کنید." to "This file is only the lists — copy the songs themselves to the new phone separately.",
        "این فایل پشتیبان MHJ Player نیست." to "This is not an MHJ Player backup file.",
        "بازیابی از فایل" to "Restore from file", "ذخیرهٔ پشتیبان" to "Save backup", "پشتیبان‌گیری" to "Backup",
        "در گوشی جدید، فایل پشتیبان را انتخاب کنید" to "On the new phone, pick the backup file",
        "در حال بازیابی…" to "Restoring…",
        "پلی‌لیست‌ها با همان ترتیب، علاقه‌مندی‌ها، نشانک‌ها و تنظیم‌ها" to "Playlists in the same order, favorites, bookmarks and settings",
        "یک فایل می‌سازد؛ آن را در تلگرام یا درایو نگه دارید" to "Makes one file — keep it in Telegram or Drive",
        "پشتیبان ذخیره شد. این فایل را به گوشی جدید بفرستید و آنجا «بازیابی از فایل» را بزنید." to "Backup saved. Send this file to the new phone and tap “Restore from file” there.",
        "برای ساختن، روی عکس آهنگ انگشت نگه دارید" to "Hold the cover picture to add one",
        "روی عکس آهنگ انگشت نگه دارید تا همان لحظه نشانک شود" to "Hold the cover picture to bookmark that moment",
        "شنیده شده" to "finished", "فایل این نشانک در گوشی نیست" to "This bookmark's file is not on the phone",
        "نشانک‌ها" to "Bookmarks", "هنوز نشانکی ندارید" to "No bookmarks yet",
        "همهٔ پوشه‌ها" to "All folders", "پخش همهٔ پوشه" to "Play folder", "پوشه‌ها" to "Folders",
        "۱×" to "1×",
        // Files, folders, subtitles
        "انتقال به پوشهٔ دیگر" to "Move to another folder", "انتقال همهٔ فایل‌ها" to "Move all files",
        "تغییر نام پوشه" to "Rename folder", "حذف پوشه از دستگاه" to "Delete folder from phone",
        "نام فایل در گوشی عوض نشد" to "The file name on the phone was not changed",
        "این فایل‌ها را نمی‌شود جابه‌جا کرد" to "These files can't be moved",
        "پوشهٔ تازه در Music" to "New folder in Music", "ساختن و انتقال" to "Create and move",
        "زیرنویس" to "Subtitles", "زیرنویس از فایل…" to "Subtitle from file…", "بدون زیرنویس" to "No subtitles",
        "زیرنویس اضافه شد" to "Subtitles added", "این فایل زیرنویس خوانده نشد" to "Couldn't read this subtitle file",
        "زمرد شب" to "Emerald Night", "سرمه‌ای سلطنتی" to "Royal Navy",
        "سبز تیره با طلایی" to "Deep green with gold", "سرمه‌ای عمیق با طلایی" to "Deep navy with champagne gold",
        "بستن" to "Close", "اشتراک" to "Share", "انتقال" to "Move",
        // A-B messages
        "اول یک آهنگ پخش کنید" to "Play something first", "نقطهٔ B باید بعد از A باشد" to "Point B must be after A",
        "تکرار A-B خاموش شد" to "A-B repeat off",
    )
}
