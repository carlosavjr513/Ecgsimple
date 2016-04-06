package ecg.ecgsimple;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void MostraMsg (View view) {
        Toast.makeText(this, "Botão Pressionado", Toast.LENGTH_LONG).show();
    }

    public void IniciaPlot (View view) {
        Intent it = new Intent(this, SecondActivity.class);
        startActivity(it);
    }

    public void sairApp (View view) {
        finish();
    }
}
