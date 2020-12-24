package com.upt.cti.smartwallet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.upt.cti.smartwallet.model.Payment;
import com.upt.cti.smartwallet.ui.PaymentAdapter;

import java.util.ArrayList;
import java.util.List;

public class Payments extends AppCompatActivity {
    private TextView tStatus;
    private Button bPrevious, bNext;
    private FloatingActionButton fabAdd;
    private ListView listPayments;
    private PaymentAdapter adapter;
    private List<Payment> payments = new ArrayList<>();
    private int currentMonth;
    private SharedPreferences prefUser;

    private final static String PREF_SETTINGS = "pref_settings";
    private static final int REQ_SIGNIN = 3;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private String userUID;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        startActivityForResult(new Intent(Payments.this, SignupActivity.class), REQ_SIGNIN);


        SharedPreferences sharedPreferences = getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE);
        currentMonth = sharedPreferences.getInt("MONTH", -1);
        if(currentMonth == -1){
            currentMonth = Month.monthFromTimestamp(AppState.getCurrentTimeDate());
        }

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    userUID = user.getUid();
                    attachDBListener();
                }
            }
        };


        initializeComponents();
        onFabButtonClick();
        onListItemClick();
        onPrevButtonClick();
        onNextButtonClick();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == REQ_SIGNIN){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show();
            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void attachDBListener(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        AppState.get().setDatabaseReference(databaseReference);

        getPaymentsFromDB();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void initializeComponents(){
        tStatus = (TextView) findViewById(R.id.tStatus);
        bPrevious = (Button) findViewById(R.id.bPrevious);
        bNext = (Button) findViewById(R.id.bNext);
        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        listPayments = (ListView) findViewById(R.id.listPayments);
    }

    public void getPaymentsFromDB( ){
        databaseReference.child("wallet").child(userUID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    if(currentMonth == Month.monthFromTimestamp(snapshot.getKey())){
                        Payment payment = snapshot.getValue(Payment.class);
                        payment.setTimestamp(snapshot.getKey());
                        payments.add(payment);

                        adapter = new PaymentAdapter(Payments.this, R.layout.item_payment, payments);
                        listPayments.setAdapter(adapter);
                        tStatus.setText("Found " + payments.size() + " payments for " + Month.intToMonthName(currentMonth) + ".");

                        adapter.notifyDataSetChanged();
                    }else{
                        tStatus.setText("No payments found for " + Month.intToMonthName(currentMonth) + ".");
                    }
                } catch (Exception e) {
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                for (int i = 0; i < payments.size(); i++) {
                    if (payments.get(i).timestamp.equals(snapshot.getKey().toString()))
                        try {
                            Payment updatePayment = snapshot.getValue(Payment.class);
                            updatePayment.setTimestamp(snapshot.getKey());

                            payments.set(i, updatePayment);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                for (int i = 0; i < payments.size(); i++) {
                    if (payments.get(i).timestamp.equals(snapshot.getKey()))
                        payments.remove(i);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void onFabButtonClick(){
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppState.get().setCurrentPayment(null);
                Intent intent = new Intent(Payments.this, AddPayment.class);
                startActivity(intent);
            }
        });

    }

    public void onListItemClick() {
        listPayments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppState.get().setCurrentPayment(payments.get(position));

                startActivity(new Intent(Payments.this, AddPayment.class));
            }
        });
    }

    public void saveToSharedPreferences(int month){
        if(month == 0){
            month = 11;
        }
        if(month == 12){
            month = 0;
        }
        prefUser = getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE);

        prefUser.edit().putInt("MONTH", month).apply();
    }

    public void onPrevButtonClick(){
        bPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToSharedPreferences(currentMonth-1);
                recreate();
            }
        });
    }

    public void onNextButtonClick(){
        bNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToSharedPreferences(currentMonth+1);
                recreate();
            }
        });
    }

    public enum Month{
        January, February, March, April, May, Juny, July, August, September, October, November, December;

        public static int monthNameToInt(Month month){
            return month.ordinal();
        }

        public static Month intToMonthName(int index){
            return Month.values()[index];
        }

        public static int monthFromTimestamp(String timestamp){
            int month = Integer.parseInt(timestamp.substring(5,7));
            return month -1;
        }
    }

    public void clicked(View v) {
        switch (v.getId()) {
            case R.id.bSignOut:
                signOut();
                break;
        }
    }

    private void signOut() {
        mAuth.signOut();
        payments.clear();
        startActivity(new Intent(getApplicationContext(), SignupActivity.class));
    }


}