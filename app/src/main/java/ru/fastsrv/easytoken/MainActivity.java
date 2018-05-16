package ru.fastsrv.easytoken;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.File;
import java.math.BigInteger;

import jnr.ffi.Struct;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 *
 * @author Dmitry Markelov
 * Telegram group: https://t.me/joinchat/D62dXAwO6kkm8hjlJTR9VA
 *
 * Если есть вопросы, отвечу в телеграме
 * If you have any questions, I will answer the telegram
 *
 *    Russian:
 *    Пример включает следующие функции:
 *       - Получаем адрес кошелька
 *       - Получаем баланс Eth
 *       - Получаем баланс Токена
 *       - Получаем название Токена
 *       - Получаем символ Токена
 *       - Получаем адрес Контракта Токена
 *       - Получаем общее количество выпущеных Токенов
 *
 *
 *   English:
 *   The example includes the following functions:
 *       - Get address wallet
 *       - Get balance Eth
 *       - Get balance Token
 *       - Get Name Token
 *       - Get Symbol Token
 *       - Get contract Token address
 *       - Get supply Token
 *
 */

public class MainActivity extends AppCompatActivity {

    WalletCreate wc = new WalletCreate();

    String url = Config.addressethnode();

    Web3j web3 = Web3jFactory.build(new HttpService(url));

    String smartcontract = Config.addresssmartcontract();
    String passwordwallet = Config.passwordwallet();

    File DataDir;

    TextView ethaddress, ethbalance, tokenname, tokensymbol, tokensupply, tokenaddress, tokenbalance, tokensymbolbalance;
    EditText sendtoaddress, sendtokenvalue, sendethervalue;

    LoadingDialog loadingDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ethaddress = (TextView) findViewById(R.id.ethaddress); // Your Ether Address
        ethbalance = (TextView) findViewById(R.id.ethbalance); // Your Ether Balance

        tokenname = (TextView) findViewById(R.id.tokenname); // Token Name
        tokensymbol = (TextView) findViewById(R.id.tokensymbol); // Token Symbol
        tokensupply = (TextView) findViewById(R.id.tokensupply); // Token Supply
        tokenaddress = (TextView) findViewById(R.id.tokenaddress); // Token Address
        tokenbalance = (TextView) findViewById(R.id.tokenbalance); // Token Balance
        tokensymbolbalance = (TextView) findViewById(R.id.tokensymbolbalance);

        sendtoaddress = (EditText) findViewById(R.id.sendtoaddress); // Address for sending ether or token

        sendtokenvalue = (EditText) findViewById(R.id.SendTokenValue); // Ammount token for sending
        sendethervalue = (EditText) findViewById(R.id.SendEthValue); // Ammount ether for sending

        /**
         * Получаем полный путь к каталогу с ключами
         * Get the full path to the directory with the keys
         */
        DataDir = this.getExternalFilesDir("/keys/");
        File KeyDir = new File(this.DataDir.getAbsolutePath());

        /**
         * Проверяем есть ли кошельки
         * Check whether there are purses
         */
        File[] listfiles = KeyDir.listFiles();
        if (listfiles.length == 0 ) {
            /**
             * Если в директории файла кошелька, добавляем кошелек
             * If the directory file of the wallet, add the wallet
             */
            try {
                String fileName = WalletUtils.generateNewWalletFile(passwordwallet, DataDir, false);

                Log.i("B4A","FileName: " + DataDir.toString() + fileName);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            /**
             * Если кошелек создан, начинаем выполнение потока
             * If the wallet is created, start the thread
             */
            wc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    ///////// On Click ////////////
    /**
     * Начать выполнение потока для отправки эфира или Токена
     * Start executing thread for sending Ether or sending Token
     */
    public void onClick(View view) {
        SendingToken st = new SendingToken();
        SendingEther se = new SendingEther();
        switch (view.getId()) {
            case R.id.SendEther:
                se.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case R.id.SendToken:
                st.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
        }

    }

    public void ShowProgressDialog()
    {
        if(loadingDialog ==null) {
            loadingDialog = new LoadingDialog(this);
        }
        if(!loadingDialog.isShowing()) {
            loadingDialog.showDialog();
            loadingDialog.show();
        }
    }

    public void HideProgressDialog()
    {
        if (loadingDialog != null && loadingDialog.isShowing())
        {
            loadingDialog.cancelDialog();
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }
    //////// end on click /////////

    ///////////////////// Create and Load Wallet /////////////////
    public class WalletCreate extends AsyncTask<Void, Integer, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShowProgressDialog();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {

            /**
            // Получаем список файлов в каталоге
            // Get list files in folder
            */
            File KeyDir = new File(DataDir.getAbsolutePath());
            File[] listfiles = KeyDir.listFiles();
            File file = new File(String.valueOf(listfiles[0]));

            try {
                /**
                // Загружаем файл кошелька и получаем адрес
                // Upload the wallet file and get the address
                */
                Credentials credentials = WalletUtils.loadCredentials(passwordwallet, file);
                String address = credentials.getAddress();
                Log.i("B4A","Eth Address: " + address);

                /**
                // Получаем Баланс
                // Get balance Ethereum
                */
                EthGetBalance etherbalance = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
                String ethbalance = Convert.fromWei(String.valueOf(etherbalance.getBalance()), Convert.Unit.ETHER).toString();
                Log.i("B4A","Eth Balance: " + ethbalance);

                /**
                // Загружаем Токен
                // Download Token
                */
                TokenERC20 token = TokenERC20.load(smartcontract, web3, credentials, GAS_PRICE, GAS_LIMIT);

                /**
                // Получаем название токена
                // Get the name of the token
                */
                String tokenname = token.name().send();
                Log.i("B4A","Token Name: " + tokenname);

                /**
                // Получаем Символ Токена
                // Get Symbol marking token
                */
                String tokensymbol = token.symbol().send();
                Log.i("B4A","Symbol Token: " + tokensymbol);

                /**
                // Получаем адрес Токена
                // Get The Address Token
                */
                String tokenaddress = token.getContractAddress();
                Log.i("B4A","Address Token: " + tokenaddress);

                /**
                // Получаем общее количество выпускаемых токенов
                // Get the total amount of issued tokens
                */
                BigInteger totalSupply = token.totalSupply().send();
                Log.i("B4A","Supply Token: "+totalSupply.toString());

                /**
                // Получаем количество токенов в кошельке
                // Receive the Balance of Tokens in the wallet
                */
                BigInteger tokenbalance = token.balanceOf(address).send();
                Log.i("B4A","Balance Token: "+ tokenbalance.toString());


                JSONObject result = new JSONObject();
                result.put("ethaddress",address);
                result.put("ethbalance", ethbalance);
                result.put("tokenbalance", tokenbalance.toString());
                result.put("tokenname", tokenname);
                result.put("tokensymbol", tokensymbol);
                result.put("tokenaddress",tokenaddress);
                result.put("tokensupply", totalSupply.toString());
                return result;
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.i("B4A","ERROR:" + ex);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            HideProgressDialog();
            if(result==null)
            {
                return;
            }

            ethaddress.setText(result.get("ethaddress").toString());
            ethbalance.setText(result.get("ethbalance").toString());
            tokenname.setText(result.get("tokenname").toString());
            tokensymbol.setText(result.get("tokensymbol").toString());
            tokensupply.setText(result.get("tokensupply").toString());
            tokenaddress.setText(result.get("tokenaddress").toString());
            tokenbalance.setText(result.get("tokenbalance").toString());
            tokensymbolbalance.setText(" "+result.get("tokensymbol").toString());

        }
    }
    ////////////////// End create and load wallet ////////////////

    ///////////////////////// Sending Tokens /////////////////////
    public class SendingToken extends AsyncTask<Void, Integer, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShowProgressDialog();
        }

        @Override
        protected JSONObject doInBackground(Void... param) {

            /**
             // Получаем список файлов в каталоге
             // Get list files in folder
             */
            File KeyDir = new File(DataDir.getAbsolutePath());
            File[] listfiles = KeyDir.listFiles();
            File file = new File(String.valueOf(listfiles[0]));

            try {
                /**
                 // Загружаем файл кошелька и получаем адрес
                 // Upload the wallet file and get the address
                 */
                Credentials credentials = WalletUtils.loadCredentials(passwordwallet, file);
                String address = credentials.getAddress();
                Log.i("B4A","Eth Address: " + address);

                /**
                 * Загружаем Токен
                 * Load Token
                 */
                TokenERC20 token = TokenERC20.load(smartcontract, web3, credentials, GAS_PRICE, GAS_LIMIT);

                String status = null;
                String balance = null;

                /**
                 * Конвертируем сумму токенов в BigInteger и отправляем на указанные адрес
                 * Convert the amount of tokens to BigInteger and send to the specified address
                 */
                BigInteger sendvalue = BigInteger.valueOf(Long.parseLong(String.valueOf(sendtokenvalue.getText())));
                RemoteCall<TransactionReceipt> remoteCall = token.transfer(String.valueOf(sendtoaddress.getText()), sendvalue);

                status = remoteCall.send().getTransactionHash();
                Log.i("B4A","getTransactionHash: "+ status);

                /**
                 * Обновляем баланс Токенов
                 * Renew Token balance
                 */
                BigInteger tokenbalance = token.balanceOf(address).send();
                Log.i("B4A","Balance Token: "+ tokenbalance.toString());
                balance = tokenbalance.toString();

                /**
                 * Возвращаем из потока, Статус транзакции и баланс Токенов
                 * Returned from thread, transaction Status and Token balance
                 */
                JSONObject result = new JSONObject();
                result.put("status",status);
                result.put("balance",balance);

                return result;
            } catch (Exception ex) {Log.i("B4A","ERROR:" + ex);}

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            HideProgressDialog();
            if (result != null) {
                tokenbalance.setText(result.get("balance").toString());
                Toast toast = Toast.makeText(getApplicationContext(),result.get("status").toString(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Log.i("B4A","SendingToken result null");
            }
        }
    }
    /////////////////////// End Sending Tokens ///////////////////

    ///////////////////////// Sending Ether //////////////////////
    public class SendingEther  extends AsyncTask<Void, Integer, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShowProgressDialog();
        }

        @Override
        protected JSONObject doInBackground(Void... param) {

                /**
                 // Получаем список файлов в каталоге
                 // Get list files in folder
                 */
                File KeyDir = new File(DataDir.getAbsolutePath());
                File[] listfiles = KeyDir.listFiles();
                File file = new File(String.valueOf(listfiles[0]));

            try {
                /**
                 // Загружаем файл кошелька и получаем адрес
                 // Upload the wallet file and get the address
                 */
                Credentials credentials = WalletUtils.loadCredentials(passwordwallet, file);
                String address = credentials.getAddress();
                Log.i("B4A","Eth Address: " + address);

                /**
                 * Получаем счетчик транзакций
                 * Get count transaction
                 */
                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                /**
                 * Convert ammount ether to BigInteger
                 */
                BigInteger value = Convert.toWei(String.valueOf(sendethervalue.getText()), Convert.Unit.ETHER).toBigInteger();

                /**
                 * Транзакция
                 * Transaction
                 */
                RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(nonce, GAS_PRICE, GAS_LIMIT, String.valueOf(sendtoaddress.getText()), value);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = "0x"+ Hex.toHexString(signedMessage);
                EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue.toString()).sendAsync().get();

                /**
                 * Get Transaction Error and Hash
                 */
                Log.i("B4A","Error: "+ ethSendTransaction.getError());
                Log.i("B4A","Transaction: " + ethSendTransaction.getTransactionHash());

                /**
                 * Возвращаем из потока, Адрес и Хэш транзакции
                 * Returned from thread, Ether Address and transaction hash
                 */
                JSONObject JsonResult = new JSONObject();
                JsonResult.put("Address", address);
                JsonResult.put("TransactionHash", ethSendTransaction.getTransactionHash());

                return JsonResult;

            }catch (Exception ex) {ex.printStackTrace();}
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            HideProgressDialog();
            try {
                /**
                 * Получаем баланс Ethereum
                 * Get balance Ethereum
                 */
                EthGetBalance etherbalance = web3.ethGetBalance(result.get("Address").toString(), DefaultBlockParameterName.LATEST).sendAsync().get();
                String ethbalanceafter = Convert.fromWei(String.valueOf(etherbalance.getBalance()), Convert.Unit.ETHER).toString();
                Log.i("B4A","Eth Balance: " + ethbalanceafter);

                ethbalance.setText(ethbalanceafter);
            } catch(Exception ex) {
                ex.printStackTrace();
            }

            Toast toast = Toast.makeText(getApplicationContext(),result.get("TransactionHash").toString(), Toast.LENGTH_LONG);
            toast.show();
        }

    }
    //////////////////// End Sending Ether ///////////////////////

}
