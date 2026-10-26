package com.peptidetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "peptide_tracker";
    private static final String RECORDS_KEY = "records";
    private static final int INK = Color.rgb(31, 48, 42);
    private static final int MUTED = Color.rgb(115, 130, 122);
    private static final int GREEN = Color.rgb(36, 116, 94);
    private static final int PAPER = Color.rgb(244, 247, 245);
    private static final int LINE = Color.rgb(224, 232, 227);

    private final ArrayList<Record> records = new ArrayList<>();
    private final ArrayList<ScheduleItem> dailySchedule = new ArrayList<>();
    private LinearLayout content;
    private String selectedTab = "Overview";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        loadRecords();
        loadSchedule();
        showScreen();
    }

    private void showScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAPER);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(22), dp(16), dp(22), dp(12));
        TextView mark = text("PT", 15, GREEN, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(Color.rgb(222, 239, 230), 14));
        top.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        titleBox.addView(text("PEPTIDE TRACKER", 12, GREEN, true));
        titleBox.addView(text("Your personal logbook", 13, MUTED, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        navScroll.setBackgroundColor(Color.WHITE);
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        String[] tabs = {"Overview", "Inventory", "Doses", "Supplies", "Schedule"};
        for (String tab : tabs) {
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText(tab);
            button.setTextSize(11);
            button.setTextColor(selectedTab.equals(tab) ? GREEN : MUTED);
            button.setTypeface(null, selectedTab.equals(tab) ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setPadding(dp(2), 0, dp(2), 0);
            button.setOnClickListener(v -> {
                selectedTab = tab;
                showScreen();
            });
            nav.addView(button, new LinearLayout.LayoutParams(-2, dp(46)));
        }
        navScroll.addView(nav);
        root.addView(navScroll);
        setContentView(root);

        if ("Overview".equals(selectedTab)) showOverview();
        else if ("Schedule".equals(selectedTab)) showSchedule();
        else showRecords(selectedTab);
    }

    private void showOverview() {
        content.addView(text("Good to have it\nall in one place.", 28, INK, true), spaced(0, 0, 0, 8));
        content.addView(text("A simple record of your supplies, costs, and notes.", 14, MUTED, false), spaced(0, 0, 0, 20));

        int inventoryCount = count("Inventory");
        int doseCount = count("Doses");
        int supplyCount = count("Supplies");
        LinearLayout stats = new LinearLayout(this);
        stats.addView(statCard("ITEMS", String.valueOf(inventoryCount), "in inventory"), weighted(1, 0, 8));
        stats.addView(statCard("DOSE LOG", String.valueOf(doseCount), "entries saved"), weighted(1, 0, 8));
        stats.addView(statCard("SUPPLIES", String.valueOf(supplyCount), "tracked"), weighted(1, 0, 0));
        content.addView(stats, spaced(0, 0, 0, 20));

        double total = 0;
        for (Record record : records) if ("Inventory".equals(record.category)) total += parseAmount(record.price);
        LinearLayout valueCard = card();
        valueCard.setOrientation(LinearLayout.VERTICAL);
        valueCard.addView(text("INVENTORY SPEND", 11, MUTED, true));
        valueCard.addView(text(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(total), 27, INK, true), spaced(0, 6, 0, 4));
        valueCard.addView(text("Total of prices you’ve recorded", 13, MUTED, false));
        content.addView(valueCard, spaced(0, 0, 0, 20));

        sectionTitle("Daily schedule", dailySchedule.size() + " planned entries");
        if (dailySchedule.isEmpty()) {
            content.addView(emptyCard("No daily plan yet", "Set up a personal schedule for your planned entries."), spaced(0, 0, 0, 10));
        } else {
            for (int i = 0; i < dailySchedule.size() && i < 3; i++) {
                content.addView(scheduleCard(dailySchedule.get(i), false), spaced(0, 0, 0, 8));
            }
        }
        Button openSchedule = primaryButton(dailySchedule.isEmpty() ? "＋   Set up daily schedule" : "Manage daily schedule");
        openSchedule.setOnClickListener(v -> {
            selectedTab = "Schedule";
            showScreen();
        });
        content.addView(openSchedule, spaced(0, 0, 0, 20));

        sectionTitle("Recent activity", "Your latest saved records");
        ArrayList<Record> recent = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0 && recent.size() < 4; i--) recent.add(records.get(i));
        if (recent.isEmpty()) content.addView(emptyCard("Nothing logged yet", "Add an inventory item, dose entry, or supply to get started."));
        else for (Record record : recent) content.addView(recordCard(record, false), spaced(0, 0, 0, 10));

        Button add = primaryButton("＋   Add a record");
        add.setOnClickListener(v -> showAddDialog(null));
        content.addView(add, spaced(0, 20, 0, 0));
        content.addView(text("For personal recordkeeping only — not medical guidance.", 11, MUTED, false), spaced(0, 14, 0, 0));
    }

    private void showRecords(String category) {
        String title = "Doses".equals(category) ? "Dose log" : category;
        String subtitle = "Inventory".equals(category) ? "Quantities, costs, and storage notes" :
                "Doses".equals(category) ? "A personal history of your entries" : "Keep track of related supplies";
        content.addView(text(title, 28, INK, true), spaced(0, 0, 0, 6));
        content.addView(text(subtitle, 14, MUTED, false), spaced(0, 0, 0, 18));
        Button add = primaryButton("＋   Add " + ("Doses".equals(category) ? "dose entry" : category.toLowerCase().replace("inventory", "item").replace("supplies", "supply")));
        add.setOnClickListener(v -> showAddDialog(category));
        content.addView(add, spaced(0, 0, 0, 20));

        int found = 0;
        for (int i = records.size() - 1; i >= 0; i--) {
            Record record = records.get(i);
            if (category.equals(record.category)) {
                content.addView(recordCard(record, true), spaced(0, 0, 0, 10));
                found++;
            }
        }
        if (found == 0) content.addView(emptyCard("No " + category.toLowerCase() + " yet", "Use the button above to add your first record."));
    }

    private void showSchedule() {
        content.addView(text("Daily schedule", 28, INK, true), spaced(0, 0, 0, 6));
        content.addView(text("A personal plan that repeats each day. It does not send reminders.", 14, MUTED, false), spaced(0, 0, 0, 18));
        Button add = primaryButton("＋   Add scheduled item");
        add.setOnClickListener(v -> showScheduleDialog());
        content.addView(add, spaced(0, 0, 0, 20));

        if (dailySchedule.isEmpty()) {
            content.addView(emptyCard("No daily schedule yet", "Add an item with a planned time and amount. You can update your plan any time."));
            return;
        }
        for (ScheduleItem item : dailySchedule) {
            content.addView(scheduleCard(item, true), spaced(0, 0, 0, 10));
        }
    }

    private void showScheduleDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText name = field(form, "Peptide / item", "Name", InputType.TYPE_CLASS_TEXT);
        EditText amount = field(form, "Planned amount", "Optional", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText unit = field(form, "Unit", "e.g. mg, mcg", InputType.TYPE_CLASS_TEXT);
        form.addView(text("Planned time", 12, MUTED, true), spaced(0, 10, 0, 4));
        String[] selectedTime = {"08:00"};
        Button timeButton = new Button(this);
        timeButton.setAllCaps(false);
        timeButton.setText(formatDisplayTime(selectedTime[0]));
        timeButton.setTextColor(INK);
        timeButton.setBackground(round(Color.rgb(247, 249, 248), 12));
        timeButton.setOnClickListener(v -> {
            Calendar parsed = Calendar.getInstance();
            parsed.set(Calendar.HOUR_OF_DAY, Integer.parseInt(selectedTime[0].substring(0, 2)));
            parsed.set(Calendar.MINUTE, Integer.parseInt(selectedTime[0].substring(3, 5)));
            new TimePickerDialog(this, (picker, hour, minute) -> {
                selectedTime[0] = String.format(Locale.US, "%02d:%02d", hour, minute);
                timeButton.setText(formatDisplayTime(selectedTime[0]));
            }, parsed.get(Calendar.HOUR_OF_DAY), parsed.get(Calendar.MINUTE), false).show();
        });
        form.addView(timeButton, new LinearLayout.LayoutParams(-1, dp(48)));
        EditText notes = field(form, "Notes", "Optional", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add to daily schedule")
                .setView(formScroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String itemName = name.getText().toString().trim();
            if (itemName.isEmpty()) {
                name.setError("Enter a name");
                return;
            }
            ScheduleItem item = new ScheduleItem();
            item.name = itemName;
            item.amount = amount.getText().toString().trim();
            item.unit = unit.getText().toString().trim();
            item.time = selectedTime[0];
            item.notes = notes.getText().toString().trim();
            dailySchedule.add(item);
            sortSchedule();
            saveSchedule();
            dialog.dismiss();
            showScreen();
        }));
        dialog.show();
    }

    private View scheduleCard(ScheduleItem item, boolean deletable) {
        LinearLayout outer = card();
        outer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.addView(text(item.name, 16, INK, true));
        String amountLine = item.amount;
        if (!item.unit.isEmpty()) amountLine += (amountLine.isEmpty() ? "" : " ") + item.unit;
        if (!amountLine.isEmpty()) details.addView(text(amountLine, 13, MUTED, false), spaced(0, 4, 0, 0));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        TextView time = text(formatDisplayTime(item.time), 12, GREEN, true);
        time.setPadding(dp(10), dp(7), dp(10), dp(7));
        time.setBackground(round(Color.rgb(230, 242, 235), 20));
        row.addView(time);
        outer.addView(row);
        if (!item.notes.isEmpty()) outer.addView(text(item.notes, 12, MUTED, false), spaced(0, 10, 0, 0));
        if (deletable) {
            TextView remove = text("Remove", 12, GREEN, true);
            remove.setGravity(Gravity.RIGHT);
            remove.setPadding(0, dp(12), 0, 0);
            remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Remove scheduled item?")
                    .setMessage("This item will be removed from your daily plan.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove", (dialog, which) -> {
                        dailySchedule.remove(item);
                        saveSchedule();
                        showScreen();
                    }).show());
            outer.addView(remove);
        }
        return outer;
    }

    private String formatDisplayTime(String time) {
        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(3, 5));
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            return android.text.format.DateFormat.getTimeFormat(this).format(calendar.getTime());
        } catch (Exception ignored) {
            return time;
        }
    }

    private void showAddDialog(String categoryHint) {
        if (categoryHint == null) {
            String[] choices = {"Inventory", "Doses", "Supplies"};
            new AlertDialog.Builder(this)
                    .setTitle("What would you like to add?")
                    .setItems(choices, (dialog, which) -> showAddDialog(choices[which]))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        final String chosen = categoryHint;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));

        EditText name = field(form, "Name", "e.g. Item name", InputType.TYPE_CLASS_TEXT);
        EditText quantity = field(form, "Quantity / amount", "Optional", InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText unit = field(form, "Unit", "e.g. vials, mg, boxes", InputType.TYPE_CLASS_TEXT);
        EditText price = null;
        EditText storage = null;
        EditText date = null;
        EditText expiry = null;
        EditText notes;
        if ("Inventory".equals(chosen)) {
            price = field(form, "Price paid", "Optional", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            storage = field(form, "Storage notes", "Optional — your own notes", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            expiry = field(form, "Expiration date", "Optional", InputType.TYPE_CLASS_TEXT);
        }
        if ("Doses".equals(chosen)) {
            date = field(form, "Date", "e.g. 2026-03-18", InputType.TYPE_CLASS_TEXT);
        }
        notes = field(form, "Notes", "Optional", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText finalPrice = price;
        EditText finalStorage = storage;
        EditText finalDate = date;
        EditText finalExpiry = expiry;
        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add record")
                .setView(formScroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String itemName = name.getText().toString().trim();
            if (itemName.isEmpty()) {
                name.setError("Enter a name");
                return;
            }
            Record record = new Record();
            record.category = chosen;
            record.name = itemName;
            record.quantity = quantity.getText().toString().trim();
            record.unit = unit.getText().toString().trim();
            record.price = finalPrice == null ? "" : finalPrice.getText().toString().trim();
            record.storage = finalStorage == null ? "" : finalStorage.getText().toString().trim();
            record.date = finalDate == null ? "" : finalDate.getText().toString().trim();
            record.expiry = finalExpiry == null ? "" : finalExpiry.getText().toString().trim();
            record.notes = notes.getText().toString().trim();
            records.add(record);
            saveRecords();
            dialog.dismiss();
            showScreen();
        }));
        dialog.show();
    }

    private EditText field(LinearLayout parent, String label, String hint, int inputType) {
        parent.addView(text(label, 12, MUTED, true), spaced(0, 8, 0, 4));
        EditText input = new EditText(this);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(round(Color.rgb(247, 249, 248), 12));
        parent.addView(input, new LinearLayout.LayoutParams(-1, dp((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ? 76 : 48)));
        return input;
    }

    private View recordCard(Record record, boolean deletable) {
        LinearLayout outer = card();
        outer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(text(record.name, 16, INK, true));
        String detail = detailLine(record);
        if (!detail.isEmpty()) textGroup.addView(text(detail, 13, MUTED, false), spaced(0, 4, 0, 0));
        row.addView(textGroup, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text(record.category.equals("Doses") ? "DOSE" : record.category.equals("Supplies") ? "SUPPLY" : "ITEM", 10, GREEN, true);
        badge.setPadding(dp(9), dp(6), dp(9), dp(6));
        badge.setBackground(round(Color.rgb(230, 242, 235), 20));
        row.addView(badge);
        outer.addView(row);

        ArrayList<String> extras = new ArrayList<>();
        if (!record.price.isEmpty()) extras.add("Paid " + NumberFormat.getCurrencyInstance(Locale.getDefault()).format(parseAmount(record.price)));
        if (!record.expiry.isEmpty()) extras.add("Expires " + record.expiry);
        if (!record.date.isEmpty()) extras.add(record.date);
        if (!record.storage.isEmpty()) extras.add("Storage: " + record.storage);
        if (!record.notes.isEmpty()) extras.add(record.notes);
        if (!extras.isEmpty()) outer.addView(text(join(extras, "  •  "), 12, MUTED, false), spaced(0, 10, 0, 0));
        if (deletable) {
            TextView remove = text("Remove", 12, GREEN, true);
            remove.setGravity(Gravity.RIGHT);
            remove.setPadding(0, dp(12), 0, 0);
            remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Remove record?")
                    .setMessage("This record will be deleted from this device.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove", (dialog, which) -> {
                        records.remove(record);
                        saveRecords();
                        showScreen();
                    }).show());
            outer.addView(remove);
        }
        return outer;
    }

    private String detailLine(Record record) {
        ArrayList<String> parts = new ArrayList<>();
        if (!record.quantity.isEmpty()) parts.add(record.quantity + (record.unit.isEmpty() ? "" : " " + record.unit));
        else if (!record.unit.isEmpty()) parts.add(record.unit);
        return join(parts, " · ");
    }

    private void sectionTitle(String title, String subtitle) {
        content.addView(text(title, 19, INK, true), spaced(0, 0, 0, 3));
        content.addView(text(subtitle, 13, MUTED, false), spaced(0, 0, 0, 12));
    }

    private View statCard(String label, String number, String caption) {
        LinearLayout box = card();
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label, 9, MUTED, true));
        box.addView(text(number, 24, INK, true), spaced(0, 9, 0, 2));
        box.addView(text(caption, 10, MUTED, false));
        return box;
    }

    private View emptyCard(String title, String subtitle) {
        LinearLayout box = card();
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(20), dp(24), dp(20), dp(24));
        box.addView(text(title, 16, INK, true));
        TextView message = text(subtitle, 13, MUTED, false);
        message.setGravity(Gravity.CENTER);
        box.addView(message, spaced(0, 6, 0, 0));
        return box;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(round(GREEN, 16));
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(round(Color.WHITE, 18));
        return box;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams spaced(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams weighted(float weight, int top, int right) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, weight);
        params.setMargins(0, dp(top), dp(right), 0);
        return params;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private int count(String category) {
        int count = 0;
        for (Record record : records) if (category.equals(record.category)) count++;
        return count;
    }

    private double parseAmount(String value) {
        try { return Double.parseDouble(value.replace(",", "")); }
        catch (Exception ignored) { return 0; }
    }

    private String join(ArrayList<String> parts, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) result.append(delimiter);
            result.append(part);
        }
        return result.toString();
    }

    private void loadRecords() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString(RECORDS_KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                Record record = new Record();
                record.category = item.optString("category", "Inventory");
                record.name = item.optString("name", "");
                record.quantity = item.optString("quantity", "");
                record.unit = item.optString("unit", "");
                record.price = item.optString("price", "");
                record.storage = item.optString("storage", "");
                record.date = item.optString("date", "");
                record.expiry = item.optString("expiry", "");
                record.notes = item.optString("notes", "");
                records.add(record);
            }
        } catch (Exception ignored) { records.clear(); }
    }

    private void loadSchedule() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString("daily_schedule", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject saved = array.getJSONObject(i);
                ScheduleItem item = new ScheduleItem();
                item.name = saved.optString("name", "");
                item.amount = saved.optString("amount", "");
                item.unit = saved.optString("unit", "");
                item.time = saved.optString("time", "08:00");
                item.notes = saved.optString("notes", "");
                dailySchedule.add(item);
            }
            sortSchedule();
        } catch (Exception ignored) { dailySchedule.clear(); }
    }

    private void sortSchedule() {
        Collections.sort(dailySchedule, (left, right) -> left.time.compareTo(right.time));
    }

    private void saveSchedule() {
        JSONArray array = new JSONArray();
        for (ScheduleItem item : dailySchedule) {
            JSONObject saved = new JSONObject();
            try {
                saved.put("name", item.name);
                saved.put("amount", item.amount);
                saved.put("unit", item.unit);
                saved.put("time", item.time);
                saved.put("notes", item.notes);
                array.put(saved);
            } catch (Exception ignored) { }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("daily_schedule", array.toString()).apply();
    }

    private void saveRecords() {
        JSONArray array = new JSONArray();
        for (Record record : records) {
            JSONObject item = new JSONObject();
            try {
                item.put("category", record.category);
                item.put("name", record.name);
                item.put("quantity", record.quantity);
                item.put("unit", record.unit);
                item.put("price", record.price);
                item.put("storage", record.storage);
                item.put("date", record.date);
                item.put("expiry", record.expiry);
                item.put("notes", record.notes);
                array.put(item);
            } catch (Exception ignored) { }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(RECORDS_KEY, array.toString()).apply();
    }

    private static class ScheduleItem {
        String name = "";
        String amount = "";
        String unit = "";
        String time = "08:00";
        String notes = "";
    }

    private static class Record {
        String category = "Inventory";
        String name = "";
        String quantity = "";
        String unit = "";
        String price = "";
        String storage = "";
        String date = "";
        String expiry = "";
        String notes = "";
    }
}
