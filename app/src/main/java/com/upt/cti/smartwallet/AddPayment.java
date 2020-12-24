package com.upt.cti.smartwallet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.upt.cti.smartwallet.model.Payment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AddPayment extends AppCompatActivity {
    private EditText EPaymentName, EPaymentCost;
    private TextView TVPaymentTime;
    private Spinner paymentTypeSpinner;
    private Button BAddPayment;

    private ArrayList<String> paymentTypeList = new ArrayList<>();
    private String typeOfPaymentSelected, currentTimeDate;
    private Payment payment;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private String userUID;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment);

        currentTimeDate = getCurrentTimeDate();

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    userUID = user.getUid();
                }
            }
        };

        setPaymentTypeList();
        initializeComponents();
        getCurrentPayment();
        getTypeOfPayment();
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

    public void setPaymentTypeList(){
        paymentTypeList.add("--Please select payment type");
        paymentTypeList.add("entertainment");
        paymentTypeList.add("food");
        paymentTypeList.add("taxes");
        paymentTypeList.add("travel");
        paymentTypeList.add("other");
    }

    public void initializeComponents(){
        EPaymentName = (EditText)findViewById(R.id.EPaymentName);
        EPaymentCost = (EditText)findViewById(R.id.EPaymentCost);
        BAddPayment = (Button)findViewById(R.id.BAddPayment);
        TVPaymentTime = (TextView)findViewById(R.id.TVPaymentTime);

        TVPaymentTime.setText(currentTimeDate);

        paymentTypeSpinner = (Spinner)findViewById(R.id.paymentTypeSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, paymentTypeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentTypeSpinner.setAdapter(adapter);
    }

    public void onAddPaymentButtonClick(String time){
        if(EPaymentName.getText().toString().isEmpty() || EPaymentCost.getText().toString().isEmpty()){
            Toast.makeText(this, "Please complete all fields!", Toast.LENGTH_SHORT).show();
        }else{
            String name = EPaymentName.getText().toString();
            double cost = Double.parseDouble(EPaymentCost.getText().toString());
            AppState.get().getDatabaseReference().child("wallet").child(userUID).child(time).setValue(new Payment(name, cost, typeOfPaymentSelected));
            Toast.makeText(AddPayment.this, "Added new payment.", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    public void getTypeOfPayment(){
        paymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position==1){
                    typeOfPaymentSelected = "entertainment";
                }

                if(position==2){
                    typeOfPaymentSelected = "food";
                }

                if(position==3){
                    typeOfPaymentSelected = "taxes";
                }

                if(position==4){
                    typeOfPaymentSelected = "travel";
                }

                if(position==5){
                    typeOfPaymentSelected = "other";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public static String getCurrentTimeDate(){
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return sdfDate.format(now);
    }

    public void getCurrentPayment(){
        payment = AppState.get().getCurrentPayment();
        if(payment != null){
            EPaymentName.setText(payment.getName());
            EPaymentCost.setText(String.valueOf(payment.getCost()));
            TVPaymentTime.setText(payment.timestamp);
            try {
                paymentTypeSpinner.setSelection(paymentTypeList.indexOf(payment.getType()));
            } catch (Exception e) {
            }
        }else{
            TVPaymentTime.setText(AppState.getCurrentTimeDate());
        }
    }

    public void clicked(View view){
        switch(view.getId()){
            case R.id.BAddPayment:
                if(payment!=null){
                    onAddPaymentButtonClick(payment.timestamp);
                }else{
                    onAddPaymentButtonClick(AppState.getCurrentTimeDate());
                }
                break;
            case R.id.BDeletePayment:
                if(payment!=null){
                    databaseReference = AppState.get().getDatabaseReference();
                    databaseReference.child("wallet").child(userUID).child(payment.timestamp).removeValue();
                    this.finish();
                }else{
                    Toast.makeText(this, "Nothing to delete.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}