package ecg.ecgsimple;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void MostraMsg (View view) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("Aviso!");
        dialog.setMessage("Você Apertou o Botão.");
        dialog.show();
    }

    public void IniciaPlot (View view) {
        Intent it = new Intent(this, SecondActivity.class);
        startActivity(it);
    }

    public void sairApp (View view) {
        finish();
    }
}
