package com.example.relay.running.view

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.relay.ApplicationClass
import com.example.relay.Constants
import com.example.relay.Constants.ACTION_PAUSE_SERVICE
import com.example.relay.Constants.ACTION_STOP_SERVICE
import com.example.relay.Constants.MAP_ZOOM
import com.example.relay.Constants.POLYLINE_WIDTH
import com.example.relay.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.relay.R
import com.example.relay.databinding.FragmentRunningBinding
import com.example.relay.db.Run
import com.example.relay.running.OnBottomSheetCallbacks
import com.example.relay.running.TrackingUtility
import com.example.relay.running.models.*
import com.example.relay.running.service.*
import com.example.relay.running.service.Polyline
import com.example.relay.timetable.models.MemSchedule
import com.example.relay.ui.viewmodels.RunningViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.gun0912.tedpermission.provider.TedPermissionProvider
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Math.round
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class RunningFragment: Fragment(), EasyPermissions.PermissionCallbacks, RunningInterface, MainInterface {

    private val viewModel: RunningViewModel by viewModels()
    private lateinit var binding: FragmentRunningBinding
    private var map: GoogleMap? = null
    private lateinit var mapView: MapView

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    var locationList = mutableListOf<PathPoints>()

    var mNow: Long = 0
    var mDate: Date? = null
    var mFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var nFormat: SimpleDateFormat = SimpleDateFormat("HH:mm:ss")

    private var curTimeInMillis = 0L
    private var curDistance = 0L

    private var listener: OnBottomSheetCallbacks? = null

    var runningRecordIdx: Long = 0
    private val userIdx = ApplicationClass.prefs.getLong("userIdx", 0L)
    private var scheduleList = mutableListOf<MemSchedule>()
    private var userProfile = mutableListOf<MainRunningResult>()
    private var timeTable = mutableListOf<TimeTable>()


    var currentMarker: Marker? = null
    var markerView: View? = null

    var markerIcon = BitmapFactory.decodeResource(
        TedPermissionProvider.context.resources,
        R.drawable.img_marker
    )
    var userProfileIdx = -1L;
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRunningBinding.inflate(inflater, container, false)
        requestPermissions()
//        if (Build.VERSION.SDK_INT >= 23) {
//            TedPermission.create()
//                .setPermissionListener(this)
//                .setRationaleMessage("?????? ?????? ????????? ????????? ??????????????????.")
//                .setDeniedMessage("[??????] -> [??????]?????? ?????? ????????? ???????????????.")
//                .setDeniedCloseButtonText("??????")
//                .setGotoSettingButtonText("??????")
//                .setRationaleTitle("HELLO")
//                .setPermissions(
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//                .check()
//        }

        mapView = binding.mapView
//        mapView = MapView(requireContext())
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            moveCameraToUser()
            map = it
            addAllPolylines()
        }

//        RunningService(this).tryGetMySchedules(userIdx)
        GetMainService(this).tryGetMain(userIdx)

//        setCustomMarkerView()

        binding.btnStart1.setOnClickListener {
            RunningService(this).tryPostRunStart(userIdx) //????????? ?????? API
            startActivity(Intent(context, RunSplashActivity::class.java))
            binding.layoutTimer.visibility = View.VISIBLE
            binding.layoutBottomSheet.visibility = View.VISIBLE
            startRun()
        }

        binding.btnPause.setOnClickListener {
            startRun()
        }

        binding.btnRestart.setOnClickListener {
            startRun()
        }

        binding.btnEnd.setOnClickListener {
            endRunAndSaveToDB()
        }

        //?????? ??????(??????????????? ?????????)
        binding.imgLookBig.setOnClickListener {
            val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_running, null)
            val mBuilder = AlertDialog.Builder(context)  //?????? ??? context, R.drawable.four_rounded_shape ????????? ???????????? ??? ???
                .setView(mDialogView)
            val  mAlertDialog = mBuilder.show()

            mAlertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))  //?????? ???????????? ????????? ?????? ?????? ?????????

            val button = mDialogView.findViewById<TextView>(R.id.img_close)
                button.setOnClickListener {
                mAlertDialog.dismiss()
            }
            TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
                curTimeInMillis = it
                val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
                mDialogView.findViewById<TextView>(R.id.tv_big_time).text = formattedTime
//            var bigTime = CustomDialog(requireContext()).findViewById<View>(R.id.tv_big_time)
//            bigTime = formattedTime
            })
            TrackingService.pathPoints.observe(viewLifecycleOwner, Observer{
                pathPoints = it
                val formattedDistance = TrackingUtility.calculatePolylineLength(pathPoints.last())
                val formattedAvgDistance =
                    TrackingUtility.calculateAvgPace(pathPoints.last(), curTimeInMillis, true)
                val formattedNowDistance =
                    TrackingUtility.calculateNowPace(pathPoints.last(), curTimeInMillis, true)
                mDialogView.findViewById<TextView>(R.id.tv_big_km).text = formattedDistance.toString()
                mDialogView.findViewById<TextView>(R.id.tv_big_avg_pace).text = formattedAvgDistance
                mDialogView.findViewById<TextView>(R.id.tv_big_now_pace).text = formattedNowDistance
            })
//            CustomDialog(requireContext()).show()
        }

        subscribeToObservers()
//        supportActionBar?.elevation = 0f
//        configureBackdrop()

        return binding.root
    }

    //observer??? ???????????? ??????
    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer{
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
            if(pathPoints.size != 0){
                val formattedDistance = TrackingUtility.calculatePolylineLength(pathPoints.last())
                val formattedAvgDistance =
                    TrackingUtility.calculateAvgPace(pathPoints.last(), curTimeInMillis, true)
                val formattedNowDistance =
                    TrackingUtility.calculateNowPace(pathPoints.last(), curTimeInMillis, true)
                binding.tvDistance.text = formattedDistance.toString()
                binding.tvPace2.text = formattedAvgDistance
                binding.tvPace1.text = formattedNowDistance
            }
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedGoalTime =
                TrackingUtility.getFormattedStopWatchTime(2400000 - curTimeInMillis + 1000, true)
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            //binding.tvTimer.text = formattedGoalTime
            binding.tvTime.text = formattedTime
        })
    }

    //????????? ??????
    private fun startRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
            binding.btnPause.visibility = View.GONE
            binding.layoutWhilePause.visibility = View.VISIBLE
            binding.layoutBottomSheet.visibility = View.VISIBLE
            binding.btnStart1.setImageResource(R.drawable.img_btn_restart)
            //configureBackdrop()
        } else {
            sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
            binding.btnPause.visibility = View.VISIBLE
            binding.layoutWhilePause.visibility = View.GONE
            binding.layoutBottomSheet.visibility = View.VISIBLE
            binding.btnStart1.setImageResource(R.drawable.img_btn_pause)
            //configureBackdrop()
        }
    }

    //????????? ??????
    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        binding.layoutTimer.visibility = View.GONE
        binding.layoutBottomSheet.visibility = View.GONE
        binding.btnPause.visibility = View.VISIBLE
        binding.layoutWhilePause.visibility = View.GONE
        binding.btnStart1.setImageResource(R.drawable.img_btn_start)
        curTimeInMillis = 0
        TrackingService.timeRunInMillis.value = 0
        val time = TrackingService.timeRunInMillis.value
        Log.d("timeRunInMillis", "${time}")
        Log.d("curTimeInMillis", "${curTimeInMillis}")

//        binding.tvTimer.text = "00:00:00"
//        binding.tvTime.text = "00:00:00"
        pathPoints.clear()
        map?.clear()
//        val ft = fragmentManager!!.beginTransaction()
//        ft.detach(this).attach(this).commit()
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.layoutTimer.visibility = View.VISIBLE

        } else {
//            val bottomSheet = WindowFragment()
//            bottomSheet.show(childFragmentManager,bottomSheet.tag)
//            binding.tvStart.text = "??????"
        }
    }



    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    //????????? ?????? ??? DB ??????
    private fun endRunAndSaveToDB() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for(polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
                //binding.tvDistance.text = distanceInMeters.toString()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * 60).toInt()
            val run = Run(bmp, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
            viewModel.insertRun(run)
            Log.d("tryPostRunEnd", "${distanceInMeters / 1000f} ${locationList} ${avgSpeed} ${runningRecordIdx} ${formattedTime}")
            RunningService(this).tryPostRunEnd((distanceInMeters / 1000f),locationList,avgSpeed,runningRecordIdx,formattedTime)
            locationList.clear()

            // ?????? ??? ????????? ????????? ?????? ??????
            val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_running_end, null)
            val mBuilder = AlertDialog.Builder(context)  //?????? ??? context, R.drawable.four_rounded_shape ????????? ???????????? ??? ???
                .setView(mDialogView)
            val  mAlertDialog = mBuilder.show()

            mAlertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))  //?????? ???????????? ????????? ?????? ?????? ?????????

            val button = mDialogView.findViewById<TextView>(R.id.tv_close2)
            button.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mDialogView.findViewById<TextView>(R.id.tv_big_km2).text = (distanceInMeters / 1000f).toString()
            mDialogView.findViewById<TextView>(R.id.tv_big_time2).text = formattedTime
            mDialogView.findViewById<TextView>(R.id.tv_big_avg_pace2).text = avgSpeed.toString()

            val showToast = Toast.makeText(context, "DB ??????",Toast.LENGTH_LONG)
            showToast.show()
            stopRun()
        }
    }

    //????????? ?????? ??? ??????
    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(Color.parseColor("#FFFF3E00"))
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)

        }
    }

    //?????? ???????????? ???????????? ?????? ??? ??????
    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            val preLastLating = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val markerOptions = MarkerOptions()
            markerOptions.position(lastLatLng)
            val polylineOptions = PolylineOptions()
                .color(Color.parseColor("#FFFF3E00"))
                .width(POLYLINE_WIDTH)
                .add(preLastLating)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
            setCurrentLocation(lastLatLng)

        }
    }

    //?????? ?????? ??????
    private fun requestPermissions() {
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    //map ????????????
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    //???????????? ?????? ?????? ?????? ?????? ?????? ??????
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also{
            it.action = action
            requireContext().startService(it)
        }

    fun setOnBottomSheetCallbacks(onBottomSheetCallbacks: OnBottomSheetCallbacks) {
        this.listener = onBottomSheetCallbacks
    }

    fun closeBottomSheet() {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun openBottomSheet() {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private var mBottomSheetBehavior: BottomSheetBehavior<View?>? = null

//    private fun configureBackdrop() {
//        val fragment = childFragmentManager.findFragmentById(R.id.filter_fragment)
//
//        fragment?.view?.let {
//            BottomSheetBehavior.from(it).let { bs ->
//                bs.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
//                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
//
//                    override fun onStateChanged(bottomSheet: View, newState: Int) {
//                        listener?.onStateChanged(bottomSheet, newState)
//                    }
//                })
//
//                bs.state = BottomSheetBehavior.STATE_EXPANDED
//                mBottomSheetBehavior = bs
//            }
//        }
//    }

    //?????? ?????? ?????? ?????? ??????
    fun setCurrentLocation(location: LatLng) {
        if (currentMarker != null) currentMarker!!.remove()
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        mNow = System.currentTimeMillis()
        mDate = Date(mNow)
        val time = mFormat.format(mDate).toString()
        locationList.add(PathPoints(location.latitude, location.longitude,booleanToString(isTracking),time))
        Log.d("LocationList","${locationList}")
        markerOptions.position(currentLatLng)
        markerOptions.draggable(true)
        currentMarker = map?.addMarker(markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerIcon)))
        val cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng)
        map?.moveCamera(cameraUpdate)
    }

    private fun setCustomMarkerView() {
        markerView = LayoutInflater.from(context).inflate(com.example.relay.R.drawable.marker,null)
    }

    //isTracking ????????? ?????? ??????
    fun booleanToString(isTracking: Boolean): String{
        if (isTracking == true){
            return "running"
        } else return "pause"
    }

    //string ?????? ??? -> milliseconds??? ??????
    fun milliseconds(date: String?): Long {
        //String date_ = date;
        val sdf = SimpleDateFormat("HH:mm:ss")
        try {
            val mDate = sdf.parse(date)
            val timeInMilliseconds = mDate.time
            println("Date in milli :: $timeInMilliseconds")
            return timeInMilliseconds
        } catch (e: ParseException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return 0
    }

    //???????????? ?????? ????????? ??????
    private fun doDayOfWeek(): Int? {
        val cal: Calendar = Calendar.getInstance()
        val nWeek: Int = cal.get(Calendar.DAY_OF_WEEK)
        var rWeek: Int? = null

        if (nWeek == 1) { // ???
            rWeek = 7
        } else if (nWeek == 2) {  //???
            rWeek = 1
        } else if (nWeek == 3) {  //???
            rWeek = 2
        } else if (nWeek == 4) {  //???
            rWeek = 3
        } else if (nWeek == 5) {  //???
            rWeek = 4
        } else if (nWeek == 6) {  //???
            rWeek = 5
        } else if (nWeek == 7) {  //???
            rWeek = 6
        }
        return rWeek
    }

    override fun onPostRunStrSuccess(response: RunStrResponse) {
        Log.d("RunStart", "onPostRunStrSuccess")

        val res = response.result

        if (response.isSuccess == false){
            binding.btnStart1.setOnClickListener {
                stopRun()
                Toast.makeText(context, "${response.message}", Toast.LENGTH_SHORT).show()
            }
        }else{
            if (runningRecordIdx != null){
                runningRecordIdx = res.runningRecordIdx
                Log.d("runningRecordIdx","${runningRecordIdx}")
                if (res.goalType == "TIME"){
                    val calculateTime =
                        TrackingUtility.getFormattedStopWatchTime((res.goal * 1000).toLong(), true)
                    val calculateTimetoMillis = res.goal * 1000
                    binding.tvGoal.text = "${calculateTime}"
                    binding.km.visibility = View.GONE
                    TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
                        curTimeInMillis = it
                        val formattedGoalTime = TrackingUtility.getFormattedStopWatchTime(
                            (calculateTimetoMillis - curTimeInMillis + 1000).toLong(),
                            true
                        )
                        val formattedTime =
                            TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
                        binding.tvTime.text = formattedTime
                        binding.tvTimer.text = formattedGoalTime
                    })
                } else {
                    binding.tvGoal.text = "${res.goal} km"
                    binding.km.visibility = View.VISIBLE
                    TrackingService.pathPoints.observe(viewLifecycleOwner, Observer{
                        pathPoints = it
                        val formattedDistance =
                            TrackingUtility.calculatePolylineLength(pathPoints.last())
                        binding.tvTimer.text = (res.goal - formattedDistance).toString()
                    })
                }
            }
        }
    }

    override fun onPostRunStrFailure(message: String) {
        Log.d("RunStart", "onPostRunStrFailure")
    }

    override fun onPostRunEndSuccess(response: RunEndResponse) {
        Log.d("RunEnd", "onPostRunEndSuccess")

        val res = response.result

        if (response.isSuccess == true){
            Log.d("isSuccess", "${response.isSuccess}")
            if (res.isSuccess == "y"){
                Log.d("isSuccess", "????????????")
                Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
            }else{
                Log.d("isSuccess", "????????????")
                Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
            }
        }else {
            Log.d("isSuccess", "${response.message}")
            Log.d("isSuccess", "${runningRecordIdx}")

        }

    }

    override fun onPostRunEndFailure(message: String) {
        Log.d("RunEnd", "onPostRunEndFailure")
    }

    override fun onGetRunMainSuccess(response: MainRunningResponse) {
        Log.d("RunMain", "onGetMainSuccess")

        val res = response.result

        mNow = System.currentTimeMillis()
        mDate = Date(mNow)
        val time = nFormat.format(mDate)

        if (res != null){
            userProfile = res.clone() as MutableList<MainRunningResult>
        }

        Log.d("userProfile", "${userProfile}")

        if(userProfile.size != 0 ){
            for (i in 0 until userProfile.size){
                if (userIdx == userProfile[i].userProfile.userProfileIdx){
                    if(userProfile[i].timeTableRes != null){
                        if (milliseconds(time) >= milliseconds(userProfile[i].timeTableRes.start) && milliseconds(userProfile[i].timeTableRes.end) >= milliseconds(time)){
                            binding.btnStart1.visibility = View.VISIBLE
                            break
                        } else binding.btnStart1.visibility = View.GONE
                    } else binding.btnStart1.visibility = View.GONE
                }
            }
        }
    }

    override fun onGetRunEndFailure(message: String) {
        Log.d("RunMain", "onGetMainFailure")
    }
}