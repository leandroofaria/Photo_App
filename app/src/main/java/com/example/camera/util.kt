package com.example.camera

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun abrirConfiguracaoApp(context: Context){
    Toast.makeText(
        context,
        "Vá para Configurações e habilite todas as permissões",
        Toast.LENGTH_LONG
    ).show()

    val intentConfiguracao = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intentConfiguracao.data = Uri.parse("package:${context.packageName}")
    context.startActivity(intentConfiguracao)
}

fun exibirDialogPermissao(context: Context, listener : DialogInterface.OnClickListener){
    AlertDialog.Builder(context)
        .setMessage("Todas as permissões são necessárias para este aplicativo")
        .setCancelable(false)
        .setPositiveButton("Ok", listener)
        .create()
        .show()
}

fun View.visivel(){
    visibility = View.VISIBLE
}

fun View.invisivel(){
    visibility = View.GONE
}
