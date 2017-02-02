package stoyanov.valentin.mycar.activities;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import stoyanov.valentin.mycar.R;
import stoyanov.valentin.mycar.activities.abstracts.NewBaseActivity;
import stoyanov.valentin.mycar.realm.models.Action;
import stoyanov.valentin.mycar.realm.models.Note;
import stoyanov.valentin.mycar.realm.models.RealmNotification;
import stoyanov.valentin.mycar.realm.models.Service;
import stoyanov.valentin.mycar.realm.models.ServiceType;
import stoyanov.valentin.mycar.realm.models.Vehicle;
import stoyanov.valentin.mycar.realm.table.RealmTable;
import stoyanov.valentin.mycar.utils.DateTimePickerUtils;
import stoyanov.valentin.mycar.utils.DateUtils;
import stoyanov.valentin.mycar.utils.MoneyUtils;
import stoyanov.valentin.mycar.utils.NotificationUtils;
import stoyanov.valentin.mycar.utils.TextUtils;

public class NewServiceActivity extends NewBaseActivity {

    private TextInputLayout tilTime, tilPrice, tilType;
    private ToggleButton toggleButton;
    private LinearLayout llDateTimeNotification;
    private TextInputLayout tilOdometerNotification;
    private TextInputLayout tilNotificationDate;
    private TextInputLayout tilNotificationTime;
    private RealmResults<ServiceType> results;
    private String serviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_service);
        initComponents();
        setComponentListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            if (isInputValid()) {
                saveToRealm();
            }
            return true;
        }else if(id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void initComponents() {
        super.initComponents();
        tilDate.setHint("Date");
        tilTime = (TextInputLayout) findViewById(R.id.til_new_service_time);
        Calendar calendar = Calendar.getInstance();
        TextUtils.setTextToTil(tilTime, DateUtils.timeToString(calendar.getTime()));
        tilPrice = (TextInputLayout) findViewById(R.id.til_new_service_price);
        tilType = (TextInputLayout) findViewById(R.id.til_new_service_type);
        toggleButton = (ToggleButton) findViewById(R.id.toggle_btn_new_service);
        toggleButton.setChecked(true);
        llDateTimeNotification = (LinearLayout) findViewById(R.id.ll_date_time_notification);
        tilOdometerNotification = (TextInputLayout) findViewById(R.id.til_odometer_notification);
        tilOdometerNotification.setVisibility(View.GONE);
        tilNotificationDate = (TextInputLayout) findViewById(R.id.til_new_service_notification_date);
        tilNotificationTime = (TextInputLayout) findViewById(R.id.til_new_service_notification_time);
        TextUtils.setTextToTil(tilNotificationDate, DateUtils.dateToString(calendar.getTime()));
        TextUtils.setTextToTil(tilNotificationTime, DateUtils.timeToString(calendar.getTime()));
        TextView tvCurrentOdometer = (TextView) findViewById(R.id.tv_new_service_current_odometer);
        setCurrentOdometer(tvCurrentOdometer);
        serviceId = getIntent().getStringExtra(RealmTable.SERVICES + RealmTable.ID);
        if(serviceId != null) {
            setUpdate(true);
            setContent();
        }
        results = myRealm.where(ServiceType.class).findAll();
        AutoCompleteTextView actvType = (AutoCompleteTextView) findViewById(R.id.actv_new_service_type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_dropdown_item_1line,
                getServiceTypeNamesFromResults());
        actvType.setAdapter(adapter);
    }

    @Override
    public void setComponentListeners() {
        super.setComponentListeners();
        DateTimePickerUtils.addDatePickerListener(NewServiceActivity.this, tilNotificationDate,
                new Date(), DateTimePickerUtils.PickerLimits.MIN);
        DateTimePickerUtils.addTimePickerListener(NewServiceActivity.this, tilTime);
        DateTimePickerUtils.addTimePickerListener(NewServiceActivity.this, tilNotificationTime);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    tilOdometerNotification.animate()
                            .translationX(tilOdometerNotification.getWidth()).setDuration(200);
                    tilOdometerNotification.setVisibility(View.GONE);
                    llDateTimeNotification.animate().translationX(0).setDuration(200);
                    llDateTimeNotification.setVisibility(View.VISIBLE);
                    Calendar calendar = Calendar.getInstance();
                    TextUtils.setTextToTil(tilNotificationDate, DateUtils.dateToString(calendar.getTime()));
                    TextUtils.setTextToTil(tilNotificationTime, DateUtils.timeToString(calendar.getTime()));
                }else {
                    llDateTimeNotification.animate()
                            .translationX(llDateTimeNotification.getWidth()).setDuration(200);
                    llDateTimeNotification.setVisibility(View.GONE);
                    tilOdometerNotification.animate().translationX(0).setDuration(200);
                    tilOdometerNotification.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void setContent() {
        Service service = myRealm.where(Service.class)
                .equalTo(RealmTable.ID, serviceId)
                .findFirst();
        TextUtils.setTextToAutoComplete(tilType, service.getType().getName());
        TextUtils.setTextToTil(tilDate, DateUtils.dateToString(service.getAction().getDate()));
        TextUtils.setTextToTil(tilTime, DateUtils.timeToString(service.getAction().getDate()));
        TextUtils.setTextToTil(tilPrice, MoneyUtils.longToString(new BigDecimal(service.getAction().getPrice())));
        TextUtils.setTextToTil(tilOdometer, String.valueOf(service.getAction().getOdometer()));
        tilOdometer.setEnabled(false);
        TextUtils.setTextToTil(tilNote, service.getNote().getContent());
    }

    @Override
    public boolean isInputValid() {
        boolean result = super.isInputValid();
        boolean valid = true;
        if (DateUtils.isDateInFuture(TextUtils.getTextFromTil(tilDate),
                TextUtils.getTextFromTil(tilTime))) {
            valid = false;
            tilDate.setError("The date is in the future");
        }
        if (!NumberUtils.isCreatable(TextUtils.getTextFromTil(tilPrice))) {
            valid = false;
            tilPrice.setError("Price should be number");
        }
        /*if (ValidationUtils.isInputValid(TextUtils.getTextFromAutoComplete(tilType))) {
            valid = false;
            tilType.setError("Incorrect input");
        }*/
        return result && valid;
    }

    @Override
    public void saveToRealm() {
        final boolean isChecked = toggleButton.isChecked();
        myRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Service service;
                if (isUpdate()) {
                    service = realm.where(Service.class)
                            .equalTo(RealmTable.ID, serviceId).findFirst();
                    service.getNote().deleteFromRealm();
                    service.getAction().deleteFromRealm();
                }else {
                    serviceId = UUID.randomUUID().toString();
                    service = realm.createObject(Service.class, serviceId);
                }

                String serviceTypeValue = TextUtils.getTextFromAutoComplete(tilType);
                ServiceType type = realm.where(ServiceType.class)
                        .equalTo(RealmTable.NAME, serviceTypeValue).findFirst();
                if (type == null) {
                    type = realm.createObject(ServiceType.class, UUID.randomUUID().toString());
                    type.setName(serviceTypeValue);
                }
                service.setType(type);

                Note note = realm.createObject(Note.class, UUID.randomUUID().toString());
                note.setContent(TextUtils.getTextFromTil(tilNote));
                service.setNote(note);

                Action action = realm.createObject(Action.class, UUID.randomUUID().toString());
                Calendar calendar = Calendar.getInstance();
                Date date = DateUtils.stringToDate(TextUtils.getTextFromTil(tilDate));
                calendar.setTime(date);
                Date time = DateUtils.stringToTime(TextUtils.getTextFromTil(tilTime));
                calendar.set(Calendar.HOUR_OF_DAY, time.getHours());
                calendar.set(Calendar.MINUTE, time.getMinutes());
                action.setDate(calendar.getTime());
                long odometer = Long.parseLong(tilOdometer.getEditText()
                        .getText().toString());
                action.setOdometer(odometer);
                long price = MoneyUtils.stringToLong(TextUtils.getTextFromTil(tilPrice));
                action.setPrice(price);
                service.setAction(action);

                RealmNotification realmNotification = service.getNotification();
                if (isChecked) {
                    if (realmNotification == null) {
                        realmNotification = realm.createObject(RealmNotification.class,
                                UUID.randomUUID().toString());
                        int id;
                        Number number = realm.where(RealmNotification.class)
                                .max(RealmTable.NOTIFICATION_ID);
                        if (number == null) {
                            id = 0;
                        }else {
                            id = number.intValue() + 1;
                        }
                        realmNotification.setNotificationId(id);
                    }
                    realmNotification.setTriggered(false);
                    Date notificationDate = DateUtils.stringToDatetime(TextUtils.getTextFromTil(tilNotificationDate),
                            TextUtils.getTextFromTil(tilNotificationTime));
                    realmNotification.setNotificationDate(notificationDate);
                    service.setNotification(realmNotification);
                }else {
                    service.setTargetOdometer(
                            Long.parseLong(TextUtils.getTextFromTil(tilOdometerNotification)));
                    if (realmNotification != null) {
                        Date notificationDate = realmNotification.getNotificationDate();
                        calendar.setTime(notificationDate);

                        Notification notification = NotificationUtils.createNotification(getApplicationContext(),
                                getVehicleId(), RealmTable.SERVICES + RealmTable.ID, service.getId(),
                                ViewActivity.ViewType.INSURANCE, ViewActivity.class, "Service",
                                service.getType().getName() + " should be revised on " +
                                        DateUtils.datetimeToString(service.getNotification().getNotificationDate()),
                                R.drawable.ic_services_black);

                        NotificationUtils.cancelNotification(getApplicationContext(), realmNotification.getNotificationId(),
                                notification);
                        realmNotification.deleteFromRealm();
                    }
                }

                if (!isUpdate()) {
                    Vehicle vehicle = realm.where(Vehicle.class)
                            .equalTo(RealmTable.ID, getVehicleId())
                            .findFirst();
                    vehicle.getServices().add(service);
                    vehicle.setOdometer(odometer);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {

                if (isChecked) {
                    Service service = myRealm.where(Service.class)
                            .equalTo(RealmTable.ID, serviceId).findFirst();
                    Date notificationDate = service.getNotification().getNotificationDate();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(notificationDate);

                    Notification notification = NotificationUtils.createNotification(getApplicationContext(),
                            getVehicleId(), RealmTable.SERVICES + RealmTable.ID, service.getId(),
                            ViewActivity.ViewType.INSURANCE, ViewActivity.class, "Service",
                            service.getType().getName() + " should be revised on " +
                                    DateUtils.datetimeToString(service.getNotification().getNotificationDate()),
                            R.drawable.ic_services_black);

                    NotificationUtils.setNotificationOnDate(getApplicationContext(), notification,
                            service.getNotification().getNotificationId(), calendar.getTimeInMillis());
                }

                if (isUpdate()) {
                    showMessage("Service updated!");
                    Intent intent = new Intent(getApplicationContext(), ViewActivity.class);
                    intent.putExtra(RealmTable.ID, getVehicleId());
                    intent.putExtra(RealmTable.SERVICES + RealmTable.ID, serviceId);
                    intent.putExtra(RealmTable.TYPE, ViewActivity.ViewType.SERVICE.ordinal());
                    startActivity(intent);
                }else {
                    showMessage("New service saved!");
                }
                finish();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                showMessage("Something went wrong...");
                error.printStackTrace();
                finish();
            }
        });
    }

    private ArrayList<String> getServiceTypeNamesFromResults() {
        ArrayList<String> arrayList = new ArrayList<>(results.size());
        for (ServiceType type : results) {
            arrayList.add(type.getName());
        }
        return arrayList;
    }
}
