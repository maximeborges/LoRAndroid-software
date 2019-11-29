package es.maximeborg.lorandroid

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.microchip.android.mcp2210comm.Mcp2210Comm
import com.microchip.android.microchipusb.Constants
import com.microchip.android.microchipusb.MCP2210
import com.microchip.android.microchipusb.MicrochipUsb
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {

    private val actionUsbPermission = "com.microchip.android.USB_PERMISSION"


    // MCP2210 object used to create Mcp2210Comm object
    private lateinit var mMcp2210: MCP2210
    // Mcp2210Comm object used to call methods from library
    private lateinit var mcp2210Comm: Mcp2210Comm

    private var mPermissionIntent: PendingIntent? = null
    private val mcp2210VID = 0x4D8
    private val mcp2210PID = 0xDE

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == actionUsbPermission) {
                synchronized(this) {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (device != null) {
                        // Is USB permission has been granted, try to open a connection
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)) {
                            // Call method to set up device communication
                            val result = mMcp2210.open()
                            if (result !== Constants.SUCCESS) {
                                Log.d("MCP2210", "No MCP2210 connection")
                            } else {
                                mcp2210Comm = Mcp2210Comm(mMcp2210)
                                Log.d("MCP2210", "MCP2210 connection opened")
                            }
                        } else {
                            Log.d("MCP2210", "USB Permission Denied")
                        }
                    }
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Log.d("MCP2210", "Device Detached")
                // Close the connection
                mMcp2210.close()
                // Reset the driver
                mcp2210Comm = Mcp2210Comm(mMcp2210)
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Log.d("MCP2210", "Device Attached")
                val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (device != null) {
                    // Only try to connect if the connected device is MCP2210
                    if (device.vendorId == mcp2210VID && device.productId == mcp2210PID) {
                        when (mMcp2210.open()) {
                            Constants.SUCCESS -> {
                                Log.d("MCP2210", "MCP2210 connectedaaa")
                                mcp2210Comm = Mcp2210Comm(mMcp2210)
                            }
                            Constants.CONNECTION_FAILED -> {
                                Log.d("MCP2210", "Connection Failed")
                            }
                            Constants.NO_USB_PERMISSION -> {
                                mMcp2210.requestUsbPermission(mPermissionIntent)
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission), 0)

        // Prepare the interface
        mMcp2210 = MCP2210(this)
        mcp2210Comm = Mcp2210Comm(mMcp2210)

        // Register USB events
        val filter = IntentFilter(actionUsbPermission)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(mUsbReceiver, filter)

        var result = MicrochipUsb.getConnectedDevice(this)
        if (result == Constants.MCP2210) {
            // Open the connection
            result = mMcp2210.open()

            when (result) {
                Constants.SUCCESS -> {
                    Log.d("MCP2210", "MCP2210 connected")
                    mcp2210Comm = Mcp2210Comm(mMcp2210)

                    val txArray = byteArrayOf(10, 20, 30)
                    val byteBuffer = ByteBuffer.wrap(txArray)

                    val txBuffer = ByteBuffer.allocate(1024)
                    txBuffer.flip()
                    val rxBuffer = ByteBuffer.allocate(1024)
                    mcp2210Comm.txferSpiDataBuf(txBuffer, rxBuffer)

                }
                Constants.CONNECTION_FAILED -> {
                    Log.d("MCP2210", "Connection failed")
                }
                Constants.NO_USB_PERMISSION-> {
                    Log.d("MCP2210", "No permissions")
                    mMcp2210.requestUsbPermission(mPermissionIntent)
                }
                else -> {
                    // TODO: crash ? or better enum
                }
            }
        }

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
            R.id.nav_tools, R.id.nav_share, R.id.nav_send), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }



}