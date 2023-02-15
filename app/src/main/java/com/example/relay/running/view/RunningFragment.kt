package com.example.relay.running.view

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
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
import com.google.android.gms.maps.OnMapReadyCallback
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
class RunningFragment: Fragment(), EasyPermissions.PermissionCallbacks, RunningInterface, MainInterface, OnMapReadyCallback {

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

    private var curLoc = LatLng(0.0, 0.0)

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
//                .setRationaleMessage("위치 정보 제공이 필요한 서비스입니다.")
//                .setDeniedMessage("[설정] -> [권한]에서 권한 변경이 가능합니다.")
//                .setDeniedCloseButtonText("닫기")
//                .setGotoSettingButtonText("설정")
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
            onMapReady(it)
            moveCameraToUser()
            map = it
            addAllPolylines()
        }

//        RunningService(this).tryGetMySchedules(userIdx)
        GetMainService(this).tryGetMain(userIdx)

//        setCustomMarkerView()

        binding.btnStart1.setOnClickListener {
            RunningService(this).tryPostRunStart(userIdx) //달리기 시작 API
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

        //크게 보기(다이얼로그 띄우기)
        binding.imgLookBig.setOnClickListener {
            val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_running, null)
            val mBuilder = AlertDialog.Builder(context)  //괄호 안 context, R.drawable.four_rounded_shape 해주면 전체화면 꽉 참
                .setView(mDialogView)
            val  mAlertDialog = mBuilder.show()

            mAlertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))  //배경 투명처리 해줘야 배경 모양 드러남

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

    //observer로 변동사항 표시
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

    //달리기 시작
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

    //달리기 종료
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
//            binding.tvStart.text = "중단"
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

    //달리기 종료 시 DB 저장
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

            // 종료 시 오늘의 달리기 기록 표출
            val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_running_end, null)
            val mBuilder = AlertDialog.Builder(context)  //괄호 안 context, R.drawable.four_rounded_shape 해주면 전체화면 꽉 참
                .setView(mDialogView)
            val  mAlertDialog = mBuilder.show()

            mAlertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))  //배경 투명처리 해줘야 배경 모양 드러남

            val button = mDialogView.findViewById<TextView>(R.id.tv_close2)
            button.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mDialogView.findViewById<TextView>(R.id.tv_big_km2).text = (distanceInMeters / 1000f).toString()
            mDialogView.findViewById<TextView>(R.id.tv_big_time2).text = formattedTime
            mDialogView.findViewById<TextView>(R.id.tv_big_avg_pace2).text = avgSpeed.toString()

            val showToast = Toast.makeText(context, "DB 저장",Toast.LENGTH_LONG)
            showToast.show()
            stopRun()
        }
    }

    //달리기 추적 선 연결
    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(Color.parseColor("#FFFF3E00"))
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)

        }
    }

    //가장 최신으로 추가되는 추적 선 연결
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

    //위치 권한 허용
    private fun requestPermissions() {
        //권한 허용 전 지도 로드 시 현재 위치 받아와 지도 생성(locationManager로 현재 위치 받아옴)
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            val locationManager = activity?.getSystemService(LOCATION_SERVICE) as LocationManager?
            val location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                curLoc = LatLng(location.latitude, location.longitude)
                Log.d("curLoc", curLoc.toString())
            }
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

    //map 생명주기
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

    //비동기로 인한 작동 속도 해결 위한 코드
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

    //현재 위치 마커 표시 함수
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

    //isTracking 보내기 위한 처리
    fun booleanToString(isTracking: Boolean): String{
        if (isTracking == true){
            return "running"
        } else return "pause"
    }

    //string 시간 값 -> milliseconds로 변환
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

    //시간표에 맞는 요일로 변경
    private fun doDayOfWeek(): Int? {
        val cal: Calendar = Calendar.getInstance()
        val nWeek: Int = cal.get(Calendar.DAY_OF_WEEK)
        var rWeek: Int? = null

        if (nWeek == 1) { // 일
            rWeek = 7
        } else if (nWeek == 2) {  //월
            rWeek = 1
        } else if (nWeek == 3) {  //화
            rWeek = 2
        } else if (nWeek == 4) {  //수
            rWeek = 3
        } else if (nWeek == 5) {  //목
            rWeek = 4
        } else if (nWeek == 6) {  //금
            rWeek = 5
        } else if (nWeek == 7) {  //토
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
                Log.d("isSuccess", "목표달성")
                Toast.makeText(context, "목표달성", Toast.LENGTH_SHORT).show()
            }else{
                Log.d("isSuccess", "목표실패")
                Toast.makeText(context, "목표실패", Toast.LENGTH_SHORT).show()
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

    //처음 지도 로드 시 현재 위치 기준으로 지도 생성
    override fun onMapReady(p0: GoogleMap) {
        this.map = p0

        Log.d("readyready", "!!!!!")

        if (curLoc != LatLng(0.0, 0.0)) {
//            CameraPosition.Builder()
//                .target(curLoc)
//                .bearing(30f)
//                .tilt(45f)
//                .zoom(17f)
//                .build()
//            curLoc = LatLng(37.557667, 126.926546)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(curLoc, 10f)
            Log.d("readyready", "${curLoc}")
            map?.moveCamera(cameraUpdate)
        }
    }
}