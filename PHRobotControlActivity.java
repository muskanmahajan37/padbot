package cn.inbot.padbotphone.robot;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.inbot.padbotlib.common.UnitConversion;
import cn.inbot.padbotlib.constant.PadBotConstants.ROBOT_SPEED;
import cn.inbot.padbotlib.constant.PadBotConstants.SPEECH_ROBOT_ORDER;
import cn.inbot.padbotlib.domain.DataContainer;
import cn.inbot.padbotlib.domain.RobotConfigVo;
import cn.inbot.padbotlib.domain.RobotDefaultInfo;
import cn.inbot.padbotlib.domain.RobotVo;
import cn.inbot.padbotlib.photo.util.SpeechRecognizeTransUtils;
import cn.inbot.padbotlib.service.LocalDataService;
import cn.inbot.padbotlib.talkingdata.TalkingDataService;
import cn.inbot.padbotlib.util.LocalUtils;
import cn.inbot.padbotlib.util.StringUtils;
import cn.inbot.padbotlib.util.ToastUtils;
import cn.inbot.padbotphone.androidservice.SpeechRecognizeService;
import cn.inbot.padbotphone.androidservice.SpeechRecognizeService.IHandleSpeechRecognizeInterface;
import cn.inbot.padbotphone.androidservice.SpeechRecognizeService.SpeechOffRecognitionBinder;
import cn.inbot.padbotphone.common.PHActivity;
import cn.inbot.padbotphone.common.PadBotApplication;
import cn.inbot.padbotphone.common.PadBotApplication.IHandleRobotNotifyInterface;
import cn.inbot.padbotphone.common.RobotGestureControlView;
import cn.inbot.padbotphone.common.RobotGestureControlView.RobotGestureControlListener;
import cn.inbot.padbotphone.common.RobotLeftControlView;
import cn.inbot.padbotphone.common.RobotLeftControlView.RobotLeftControlListener;
import cn.inbot.padbotphone.common.RobotRightControlView;
import cn.inbot.padbotphone.common.RobotRightControlView.RobotRightControlListener;
import cn.inbot.padbotphone.service.BluetoothService;
import com.inbot.module.padbot.robot.PHRobotConfigActivity;
import com.tendcloud.tenddata.TCAgent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint({"NewApi"})
public class PHRobotControlActivity
  extends PHActivity
  implements PadBotApplication.IHandleRobotNotifyInterface, SpeechRecognizeService.IHandleSpeechRecognizeInterface
{
  private static final int REQUEST_ENABLE_BT = 1;
  private static final int ROBOT_HEART_BEAT_PERIOD = 600;
  private static final int ROBOT_ORDER_EXECUTE_PERIOD = 50;
  private static final String TAG = "robotControl";
  private static boolean isChargeFinding;
  private static boolean isChargeSearching;
  private static boolean isChargeing;
  private boolean autoInfra;
  private Button beginChargeButton;
  private ImageView cartoonBalteryImageView;
  private ImageView chargeButtonImageView;
  private ImageView chargeImageView;
  private Handler chargeLayoutHandle;
  private AnimationDrawable chargingAnimation;
  private ImageView configImageView;
  private ServiceConnection conn = new ServiceConnection()
  {
    public void onServiceConnected(ComponentName paramAnonymousComponentName, IBinder paramAnonymousIBinder)
    {
      PHRobotControlActivity.this.speechOffLineRecognizeService = ((SpeechRecognizeService.SpeechOffRecognitionBinder)paramAnonymousIBinder).getService();
      PHRobotControlActivity.this.speechOffLineRecognizeService.setHandleSpeechRecognizeInterface(PHRobotControlActivity.this);
    }
    
    public void onServiceDisconnected(ComponentName paramAnonymousComponentName)
    {
      PHRobotControlActivity.this.speechOffLineRecognizeService.stopRecognize();
      PHRobotControlActivity.this.speechOffLineRecognizeService.cancelRecognize();
      PHRobotControlActivity.this.speechOffLineRecognizeService.setHandleSpeechRecognizeInterface(null);
      PHRobotControlActivity.this.unbindService(PHRobotControlActivity.this.conn);
    }
  };
  private String currenrname;
  private Button endChargeButton;
  private boolean isInSpeechControl;
  private boolean isInTouchControl;
  private boolean isSearchChargeAnimationRunning;
  private boolean isSearchChargingAnimationRunning;
  private boolean isSpeechControlEnable;
  private boolean isUseSpeechFunction;
  private int lastGestureOrder;
  private int lastLeftOrder;
  private int lastObstacleFlag;
  private int lastRightOrder;
  private ImageView notChargeImageView;
  private Handler notifyDisconnectHandler = new Handler()
  {
    public void handleMessage(Message paramAnonymousMessage)
    {
      PHRobotControlActivity.this.obstacleInfraLayout.setVisibility(8);
      PHRobotControlActivity.this.robotChargeLayout.setVisibility(8);
      PHRobotControlActivity.this.powerLayout.setVisibility(8);
      PHRobotControlActivity.this.chargeButtonImageView.setVisibility(8);
      PHRobotControlActivity.this.configImageView.setVisibility(8);
      PHRobotControlActivity.this.closeSpeechFunction();
      PHRobotControlActivity.this.lastObstacleFlag = -1;
    }
  };
  private Handler notifyVersionHandler = new Handler()
  {
    public void handleMessage(Message paramAnonymousMessage)
    {
      if (paramAnonymousMessage.what > 1001)
      {
        if ((!PHRobotControlActivity.isChargeFinding) && (!PHRobotControlActivity.isChargeSearching)) {
          PHRobotControlActivity.this.chargeButtonImageView.setVisibility(0);
        }
        paramAnonymousMessage = DataContainer.getDataContainer().getCurrentRobotVo();
        if ((paramAnonymousMessage != null) && (!StringUtils.isEmpty(paramAnonymousMessage.getRobotName()))) {
          break label204;
        }
        PHRobotControlActivity.this.configImageView.setVisibility(8);
        PHRobotControlActivity.this.closeSpeechFunction();
      }
      for (;;)
      {
        PHRobotControlActivity.this.robotOrderExecuteBufferStr = new StringBuffer("");
        if (PHRobotControlActivity.this.robotOrderExecuteTimerTask != null)
        {
          PHRobotControlActivity.this.robotOrderExecuteTimerTask.cancel();
          PHRobotControlActivity.this.robotOrderExecuteTimerTask = null;
        }
        if (PHRobotControlActivity.this.robotOrderExecuteTimerTask == null)
        {
          PHRobotControlActivity.this.robotOrderExecuteTimerTask = new TimerTask()
          {
            public void run()
            {
              PHRobotControlActivity.this.executeRobotOrder();
            }
          };
          if (PHRobotControlActivity.this.robotOrderExecuteTimer == null) {
            PHRobotControlActivity.this.robotOrderExecuteTimer = new Timer();
          }
          PHRobotControlActivity.this.robotOrderExecuteTimer.schedule(PHRobotControlActivity.this.robotOrderExecuteTimerTask, 0L, 50L);
        }
        return;
        PHRobotControlActivity.this.chargeButtonImageView.setVisibility(8);
        break;
        label204:
        if (paramAnonymousMessage.getRobotVersion() < 1001)
        {
          PHRobotControlActivity.this.configImageView.setVisibility(8);
        }
        else
        {
          PHRobotControlActivity.this.configImageView.setVisibility(0);
          if (PHRobotControlActivity.this.isUseSpeechFunction) {
            PHRobotControlActivity.this.speechImageView.setVisibility(0);
          }
        }
      }
    }
  };
  private Drawable obstacleBackdrawable_4;
  private int obstacleFlag = 3;
  Handler obstacleHandle = new Handler()
  {
    public void handleMessage(Message paramAnonymousMessage)
    {
      int i = paramAnonymousMessage.what;
      if (i == 0)
      {
        if (8 != PHRobotControlActivity.this.obstacleInfraLayout.getVisibility()) {
          PHRobotControlActivity.this.obstacleInfraLayout.setVisibility(8);
        }
        return;
      }
      if ((!PHRobotControlActivity.isChargeing) && (!PHRobotControlActivity.isChargeSearching) && (PHRobotControlActivity.this.autoInfra) && (PHRobotControlActivity.this.obstacleInfraLayout.getVisibility() != 0)) {
        PHRobotControlActivity.this.obstacleInfraLayout.setVisibility(0);
      }
      PHRobotControlActivity.this.setObstacleInfraImageView(i);
    }
  };
  private ImageView obstacleInfraImageView;
  private RelativeLayout obstacleInfraLayout;
  private Drawable obstacleLeftBackdrawable_10;
  private Drawable obstacleLeftRightBackdrawable_7;
  private Drawable obstacleLeftRightdrawable_5;
  private Drawable obstacleLeftdrawable_1;
  private Drawable obstacleMiddledrawable_3;
  private Drawable obstacleRightBackdrawable_11;
  private Drawable obstacleRightdrawable_2;
  private ImageView poinSpeechModeImageView;
  private ImageView[] pointCursorImageViews;
  private LinearLayout pointGroupLayout;
  private ImageView pointHandModeImageView;
  private List<View> pointPageViewList;
  private ViewPager pointViewPager;
  private ImageView powerChargeImageView;
  private Drawable powerDrawable_0;
  private Drawable powerDrawable_1;
  private Drawable powerDrawable_2;
  private Drawable powerDrawable_3;
  private Drawable powerDrawable_4;
  Handler powerHandler = new Handler()
  {
    public void handleMessage(Message paramAnonymousMessage)
    {
      paramAnonymousMessage = paramAnonymousMessage.getData();
      int i = paramAnonymousMessage.getInt("voltage");
      double d = paramAnonymousMessage.getDouble("volFlag");
      PHRobotControlActivity.this.setPowerImageView(d);
      PHRobotControlActivity.this.powerValueTextView.setText(i + "%");
      PHRobotControlActivity.this.powerLayout.setVisibility(0);
    }
  };
  private ImageView powerImageView;
  private LinearLayout powerLayout;
  private TextView powerValueTextView;
  private RelativeLayout robotChargeLayout;
  private int robotChargeLayoutWidth;
  private ImageView robotFirstImageView;
  private RobotGestureControlView robotGestureControlView;
  private TimerTask robotHeartBeatTask;
  private Timer robotHeartBeatTimer;
  private Timer robotInfraTimer;
  private TimerTask robotInfraTimerTask;
  private RobotLeftControlView robotLeftControlView;
  private int robotMarginLeftRight;
  private StringBuffer robotOrderExecuteBufferStr;
  private Timer robotOrderExecuteTimer;
  private TimerTask robotOrderExecuteTimerTask;
  private RobotRightControlView robotRightControlView;
  private ImageView robotSeatImageView;
  private ImageView robotThirdImageView;
  private int robotViewWidth;
  private Animation searchChargeAnimation;
  private TranslateAnimation speechBackwardAnimation;
  private Handler speechControlHandler = new Handler()
  {
    public void handleMessage(Message paramAnonymousMessage)
    {
      if (paramAnonymousMessage.what == 10) {
        PHRobotControlActivity.this.stopSpeechControlMoveWithoutShowPoint();
      }
      while (paramAnonymousMessage.what != 11) {
        return;
      }
      PHRobotControlActivity.this.dismissSpeechPoint();
    }
  };
  private RelativeLayout speechControlLayout;
  private Timer speechControlStopTimer;
  private TimerTask speechControlStopTimerTask;
  private TimerTask speechDismissPointTimerTask;
  private TranslateAnimation speechForwardAnimation;
  private ImageView speechImageView;
  private ImageView speechInputImageView;
  private TranslateAnimation speechLeftAnimation;
  private AnimationDrawable speechListeningAnimation;
  private SpeechRecognizeService speechOffLineRecognizeService;
  private AnimationDrawable speechPointAnimation;
  private ImageView speechPointImageView;
  private TranslateAnimation speechRightAnimation;
  private Timer stopOrderTime;
  private TimerTask stopOrderTimerTask;
  private RelativeLayout touchControlLayout;
  private Handler unChargeHandler;
  private Runnable unChargeRunable = new Runnable()
  {
    public void run()
    {
      PHRobotControlActivity.this.chargeButtonImageView.setVisibility(0);
      PHRobotControlActivity.this.robotChargeLayout.setVisibility(8);
      PHRobotControlActivity.this.findNoChargeBase();
    }
  };
  
  private void closeSpeechFunction()
  {
    if (this.isUseSpeechFunction)
    {
      this.speechOffLineRecognizeService.stopRecognize();
      this.isSpeechControlEnable = false;
      this.speechImageView.setImageResource(2130837638);
      this.robotLeftControlView.show();
      this.robotRightControlView.show();
      this.speechImageView.setVisibility(8);
      this.speechControlLayout.setVisibility(8);
      stopSpeechControlMoveWithoutShowPoint();
      stopSpeechListeningAnimation();
    }
  }
  
  private void dismissSpeechPoint()
  {
    if (this.speechPointImageView.getVisibility() == 0) {
      this.speechPointImageView.setVisibility(8);
    }
    if (this.speechDismissPointTimerTask != null)
    {
      this.speechDismissPointTimerTask.cancel();
      this.speechDismissPointTimerTask = null;
    }
  }
  
  private void executeRobotOrder()
  {
    RobotVo localRobotVo = DataContainer.getDataContainer().getCurrentRobotVo();
    if ((localRobotVo != null) && (this.robotOrderExecuteBufferStr != null) && (StringUtils.isNotEmpty(this.robotOrderExecuteBufferStr.toString())))
    {
      Log.i("robotControl", "sendOrderToRobot:" + this.robotOrderExecuteBufferStr.toString());
      BluetoothService.getInstance().sendInstruction(localRobotVo, this.robotOrderExecuteBufferStr.toString());
      this.robotOrderExecuteBufferStr.delete(0, this.robotOrderExecuteBufferStr.length());
    }
  }
  
  private void initSpeedLevel()
  {
    if (DataContainer.getDataContainer().getRobotConfigVo() != null)
    {
      int i = DataContainer.getDataContainer().getRobotConfigVo().getRobotSpeedType();
      if (PadBotConstants.ROBOT_SPEED.ROBOT_SPEED_FASTER.ordinal() == i)
      {
        sendRobotOrder("]");
        return;
      }
      if (PadBotConstants.ROBOT_SPEED.ROBOT_SPEED_FAST.ordinal() == i)
      {
        sendRobotOrder("W");
        return;
      }
      if (PadBotConstants.ROBOT_SPEED.ROBOT_SPEED_MID.ordinal() == i)
      {
        sendRobotOrder("E");
        return;
      }
      sendRobotOrder("D");
      return;
    }
    sendRobotOrder("D");
  }
  
  private static boolean isTablet(Context paramContext)
  {
    return (paramContext.getResources().getConfiguration().screenLayout & 0xF) >= 3;
  }
  
  private void prepareDismissSpeechPoint(int paramInt)
  {
    if (this.speechDismissPointTimerTask != null)
    {
      this.speechDismissPointTimerTask.cancel();
      this.speechDismissPointTimerTask = null;
    }
    this.speechDismissPointTimerTask = new TimerTask()
    {
      public void run()
      {
        PHRobotControlActivity.this.speechControlHandler.sendEmptyMessage(11);
      }
    };
    this.speechControlStopTimer.schedule(this.speechDismissPointTimerTask, paramInt);
  }
  
  private void sendOrderToRobot(int paramInt1, int paramInt2)
  {
    String str2 = "0";
    String str1;
    if (paramInt1 == 1) {
      if (paramInt2 == 0)
      {
        str1 = "X1";
        Log.i("robotControl", "forward");
      }
    }
    for (;;)
    {
      sendRobotOrder(str1);
      return;
      if (paramInt2 == 3)
      {
        str1 = "XF";
        Log.i("robotControl", "forward left 10");
      }
      else if (paramInt2 == 4)
      {
        str1 = "XG";
        Log.i("robotControl", "forward left 20");
      }
      else if (paramInt2 == 5)
      {
        str1 = "XH";
        Log.i("robotControl", "forward left 30");
      }
      else if (paramInt2 == 6)
      {
        str1 = "XI";
        Log.i("robotControl", "forward left 40");
      }
      else if (paramInt2 == 7)
      {
        str1 = "XJ";
        Log.i("robotControl", "forward right 10");
      }
      else if (paramInt2 == 8)
      {
        str1 = "XK";
        Log.i("robotControl", "forward right 20");
      }
      else if (paramInt2 == 9)
      {
        str1 = "XL";
        Log.i("robotControl", "forward right 30");
      }
      else
      {
        str1 = str2;
        if (paramInt2 == 10)
        {
          str1 = "XM";
          Log.i("robotControl", "forward right 40");
          continue;
          if (paramInt1 == 2)
          {
            if (paramInt2 == 0)
            {
              str1 = "X4";
              Log.i("robotControl", "backwrad");
            }
            else if (paramInt2 == 3)
            {
              str1 = "XN";
              Log.i("robotControl", "backwrad left 10");
            }
            else if (paramInt2 == 4)
            {
              str1 = "XO";
              Log.i("robotControl", "backwrad left 20");
            }
            else if (paramInt2 == 5)
            {
              str1 = "XP";
              Log.i("robotControl", "backwrad left 30");
            }
            else if (paramInt2 == 6)
            {
              str1 = "XQ";
              Log.i("robotControl", "backwrad left 40");
            }
            else if (paramInt2 == 7)
            {
              str1 = "XR";
              Log.i("robotControl", "backwrad right 10");
            }
            else if (paramInt2 == 8)
            {
              str1 = "XS";
              Log.i("robotControl", "backwrad right 20");
            }
            else if (paramInt2 == 9)
            {
              str1 = "XT";
              Log.i("robotControl", "backwrad right 30");
            }
            else
            {
              str1 = str2;
              if (paramInt2 == 10)
              {
                str1 = "XU";
                Log.i("robotControl", "backwrad right 40");
              }
            }
          }
          else
          {
            str1 = str2;
            if (paramInt1 == 0) {
              if ((DataContainer.getDataContainer().getCurrentRobotVo() != null) && (DataContainer.getDataContainer().getCurrentRobotVo().getRobotVersion() < 1001))
              {
                if (paramInt2 == 0)
                {
                  str1 = "0";
                  Log.i("robotControl", "stop");
                }
                else if (paramInt2 == 3)
                {
                  str1 = "X2";
                  Log.i("robotControl", "left");
                }
                else if (paramInt2 == 4)
                {
                  str1 = "X2";
                  Log.i("robotControl", "left");
                }
                else if (paramInt2 == 5)
                {
                  str1 = "X2";
                  Log.i("robotControl", "left");
                }
                else if (paramInt2 == 6)
                {
                  str1 = "X2";
                  Log.i("robotControl", "left");
                }
                else if (paramInt2 == 7)
                {
                  str1 = "X3";
                  Log.i("robotControl", "right");
                }
                else if (paramInt2 == 8)
                {
                  str1 = "X3";
                  Log.i("robotControl", "right");
                }
                else if (paramInt2 == 9)
                {
                  str1 = "X3";
                  Log.i("robotControl", "right");
                }
                else
                {
                  str1 = str2;
                  if (paramInt2 == 10)
                  {
                    str1 = "X3";
                    Log.i("robotControl", "right");
                  }
                }
              }
              else if (paramInt2 == 0)
              {
                str1 = "0";
                Log.i("robotControl", "stop");
              }
              else if (paramInt2 == 3)
              {
                str1 = "XB";
                Log.i("robotControl", "left half");
              }
              else if (paramInt2 == 4)
              {
                str1 = "XB";
                Log.i("robotControl", "left half");
              }
              else if (paramInt2 == 5)
              {
                str1 = "XB";
                Log.i("robotControl", "left half");
              }
              else if (paramInt2 == 6)
              {
                str1 = "X2";
                Log.i("robotControl", "left");
              }
              else if (paramInt2 == 7)
              {
                str1 = "XC";
                Log.i("robotControl", "right half");
              }
              else if (paramInt2 == 8)
              {
                str1 = "XC";
                Log.i("robotControl", "right half");
              }
              else if (paramInt2 == 9)
              {
                str1 = "XC";
                Log.i("robotControl", "right half");
              }
              else
              {
                str1 = str2;
                if (paramInt2 == 10)
                {
                  str1 = "X3";
                  Log.i("robotControl", "right");
                }
              }
            }
          }
        }
      }
    }
  }
  
  private void sendRobotOrder(String paramString)
  {
    if ((DataContainer.getDataContainer().getCurrentRobotVo() == null) || (this.robotOrderExecuteBufferStr == null)) {}
    for (;;)
    {
      return;
      this.robotOrderExecuteBufferStr.append(paramString);
      if ((!"X".equals(paramString)) && (!paramString.equals("&")) && (!paramString.equals(":")) && (!paramString.equals(";")) && (!paramString.equals("?")) && (!paramString.equals("D")) && (!paramString.equals("E")) && (!paramString.equals("V")) && (!paramString.equals("W")) && (!paramString.equals("[")) && (!paramString.equals("]")) && (!paramString.equals("Y")) && (!paramString.equals("Z")))
      {
        if (!paramString.equals("0")) {
          break label305;
        }
        if (this.robotHeartBeatTask != null)
        {
          this.robotHeartBeatTask.cancel();
          this.robotHeartBeatTask = null;
          Log.i("robotControl", "robotHeartBeatTask set nil");
        }
        if (this.stopOrderTime == null) {
          this.stopOrderTime = new Timer();
        }
        if (this.stopOrderTimerTask == null)
        {
          this.stopOrderTimerTask = new TimerTask()
          {
            public void run()
            {
              PHRobotControlActivity.this.sendRobotOrder("0");
            }
          };
          this.stopOrderTime.schedule(this.stopOrderTimerTask, 911L);
        }
        new Timer().schedule(new TimerTask()
        {
          public void run()
          {
            if (PHRobotControlActivity.this.stopOrderTimerTask != null)
            {
              PHRobotControlActivity.this.stopOrderTimerTask.cancel();
              PHRobotControlActivity.this.stopOrderTimerTask = null;
              Log.i("robotControl", "stopOrderTimerTask set nil");
            }
          }
        }, 3330L);
      }
      while ((this.isSpeechControlEnable) && (paramString.equals("0")) && (this.speechControlStopTimerTask != null))
      {
        this.speechControlStopTimerTask.cancel();
        this.speechControlStopTimerTask = null;
        return;
        label305:
        if ((paramString.equals("<")) || (paramString.equals(">")) || (paramString.equals("%")) || (paramString.equals("+")) || (paramString.equals("-")) || (paramString.equals("(")) || (paramString.equals(")")) || (paramString.equals(",")))
        {
          if (this.robotHeartBeatTask != null)
          {
            this.robotHeartBeatTask.cancel();
            this.robotHeartBeatTask = null;
            Log.i("robotControl", "robotHeartBeatTask set nil");
          }
        }
        else if ((paramString.equals("X5")) || (paramString.equals("XA")) || (paramString.equals("X1")) || (paramString.equals("X2")) || (paramString.equals("X3")) || (paramString.equals("X4")) || (paramString.equals("X6")) || (paramString.equals("X7")) || (paramString.equals("X8")) || (paramString.equals("X9")) || (paramString.equals("XB")) || (paramString.equals("XC")) || (paramString.equals("XF")) || (paramString.equals("XG")) || (paramString.equals("XH")) || (paramString.equals("XI")) || (paramString.equals("XJ")) || (paramString.equals("XK")) || (paramString.equals("XL")) || (paramString.equals("XM")) || (paramString.equals("XN")) || (paramString.equals("XO")) || (paramString.equals("XP")) || (paramString.equals("XQ")) || (paramString.equals("XR")) || (paramString.equals("XS")) || (paramString.equals("XT")) || (paramString.equals("XU")))
        {
          if (this.robotHeartBeatTask != null)
          {
            this.robotHeartBeatTask.cancel();
            this.robotHeartBeatTask = null;
            Log.i("robotControl", "robotHeartBeatTask set nil");
          }
          if (this.robotHeartBeatTask == null)
          {
            this.robotHeartBeatTask = new TimerTask()
            {
              public void run()
              {
                PHRobotControlActivity.this.sendRobotOrder("X");
              }
            };
            this.robotHeartBeatTimer.schedule(this.robotHeartBeatTask, 600L, 600L);
            Log.i("robotControl", "robotHeartBeatTask build");
          }
          if (this.isSpeechControlEnable)
          {
            if (this.speechControlStopTimerTask != null)
            {
              this.speechControlStopTimerTask.cancel();
              this.speechControlStopTimerTask = null;
            }
            if (this.speechDismissPointTimerTask != null)
            {
              this.speechDismissPointTimerTask.cancel();
              this.speechDismissPointTimerTask = null;
            }
          }
        }
      }
    }
  }
  
  private void setupOperatePointImage()
  {
    if (getResources().getConfiguration().orientation == 2) {
      if (LocalUtils.isCurrentLanguageSimplifiedChinese())
      {
        this.pointHandModeImageView.setImageResource(2130837623);
        if (this.poinSpeechModeImageView != null) {
          this.poinSpeechModeImageView.setImageResource(2130837659);
        }
      }
    }
    do
    {
      do
      {
        do
        {
          do
          {
            return;
            this.pointHandModeImageView.setImageResource(2130837622);
          } while (this.poinSpeechModeImageView == null);
          this.poinSpeechModeImageView.setImageDrawable(null);
          return;
        } while (getResources().getConfiguration().orientation != 1);
        if (!LocalUtils.isCurrentLanguageSimplifiedChinese()) {
          break;
        }
        this.pointHandModeImageView.setImageResource(2130837625);
      } while (this.poinSpeechModeImageView == null);
      this.poinSpeechModeImageView.setImageResource(2130837660);
      return;
      this.pointHandModeImageView.setImageResource(2130837624);
    } while (this.poinSpeechModeImageView == null);
    this.poinSpeechModeImageView.setImageDrawable(null);
  }
  
  private void showPromptInfoAlert(String paramString)
  {
    final AlertDialog localAlertDialog = new AlertDialog.Builder(this).create();
    localAlertDialog.show();
    Window localWindow = localAlertDialog.getWindow();
    localWindow.setContentView(2130903088);
    UnitConversion.setLinearLayoutParams((LinearLayout)localWindow.findViewById(2131362217), localWindow.getContext(), 180, 540);
    TextView localTextView = (TextView)localWindow.findViewById(2131362218);
    localTextView.setText(paramString);
    UnitConversion.setTextViewFontSize(localTextView, localWindow.getContext(), 12);
    UnitConversion.setViewPadding(localTextView, localWindow.getContext(), 20, 0, 20, 0);
    UnitConversion.setLinearLayoutParams(localTextView, localWindow.getContext(), 50, 0);
    paramString = (TextView)localWindow.findViewById(2131362219);
    paramString.setText(Html.fromHtml("<a href=\"https://play.google.com/store/apps/details?id=com.manuelnaranjo.btle.installer2&hl=en\"><u>https://play.google.com/store/apps/details?id=com.manuelnaranjo.btle.installer2&hl=en</u></a>"));
    paramString.setMovementMethod(LinkMovementMethod.getInstance());
    UnitConversion.setTextViewFontSize(paramString, localWindow.getContext(), 12);
    UnitConversion.setViewPadding(paramString, localWindow.getContext(), 20, 0, 20, 10);
    UnitConversion.setLinearLayoutParams(paramString, localWindow.getContext(), 70, 0);
    paramString = (Button)localWindow.findViewById(2131362221);
    UnitConversion.setButtonFontSize(paramString, localWindow.getContext(), 12);
    UnitConversion.setLinearLayoutParams(paramString, localWindow.getContext(), 60, 0);
    paramString.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View paramAnonymousView)
      {
        localAlertDialog.cancel();
      }
    });
  }
  
  private void startAutoCharge()
  {
    if (this.unChargeHandler != null)
    {
      this.unChargeHandler.removeCallbacks(this.unChargeRunable);
      this.unChargeHandler = null;
    }
    if (this.robotChargeLayout.getVisibility() != 0) {
      this.robotChargeLayout.setVisibility(0);
    }
    hideFindChargeBase();
    stopChargingAnimation();
    startSearchChargeAnimation();
    isChargeSearching = true;
    isChargeing = false;
    if (this.beginChargeButton.getVisibility() == 0) {
      this.beginChargeButton.setVisibility(4);
    }
    if (this.endChargeButton.getVisibility() != 0) {
      this.endChargeButton.setVisibility(0);
    }
    RobotVo localRobotVo = DataContainer.getDataContainer().getCurrentRobotVo();
    BluetoothService.getInstance().sendInstruction(localRobotVo, "<");
  }
  
  private void startChargingAnimation()
  {
    if (this.chargeImageView.getVisibility() == 0) {
      this.chargeImageView.setVisibility(4);
    }
    if (this.notChargeImageView.getVisibility() == 0) {
      this.notChargeImageView.setVisibility(4);
    }
    if (this.robotFirstImageView.getVisibility() == 0) {
      this.robotFirstImageView.setVisibility(4);
    }
    if (this.robotThirdImageView.getVisibility() != 0) {
      this.robotThirdImageView.setVisibility(0);
    }
    if (this.cartoonBalteryImageView.getVisibility() != 0) {
      this.cartoonBalteryImageView.setVisibility(0);
    }
    if (this.chargingAnimation == null)
    {
      this.cartoonBalteryImageView.setBackgroundResource(2130968585);
      this.chargingAnimation = ((AnimationDrawable)this.cartoonBalteryImageView.getBackground());
      this.chargingAnimation.setOneShot(false);
    }
    if (!this.chargingAnimation.isRunning()) {
      this.chargingAnimation.start();
    }
  }
  
  private void startSpeechBackwardPointAnimation()
  {
    if (this.speechBackwardAnimation == null)
    {
      this.speechBackwardAnimation = new TranslateAnimation(0.0F, 0.0F, -150.0F, 150.0F);
      this.speechBackwardAnimation.setDuration(1000L);
      this.speechBackwardAnimation.setStartOffset(0L);
      this.speechBackwardAnimation.setFillAfter(false);
      this.speechBackwardAnimation.setRepeatCount(-1);
    }
    for (;;)
    {
      this.speechPointImageView.setBackgroundResource(2130837644);
      this.speechPointImageView.startAnimation(this.speechBackwardAnimation);
      return;
      this.speechBackwardAnimation.reset();
    }
  }
  
  private void startSpeechDownHeadPointAnimation()
  {
    this.speechPointImageView.setBackgroundResource(2130968590);
    this.speechPointAnimation = ((AnimationDrawable)this.speechPointImageView.getBackground());
    this.speechPointAnimation.setOneShot(false);
    this.speechPointAnimation.start();
  }
  
  private void startSpeechForwardPointAnimation()
  {
    if (this.speechForwardAnimation == null)
    {
      this.speechForwardAnimation = new TranslateAnimation(0.0F, 0.0F, 150.0F, -150.0F);
      this.speechForwardAnimation.setDuration(1000L);
      this.speechForwardAnimation.setStartOffset(0L);
      this.speechForwardAnimation.setFillAfter(false);
      this.speechForwardAnimation.setRepeatCount(-1);
    }
    for (;;)
    {
      this.speechPointImageView.setBackgroundResource(2130837645);
      this.speechPointImageView.startAnimation(this.speechForwardAnimation);
      return;
      this.speechForwardAnimation.reset();
    }
  }
  
  private void startSpeechLeftPointAnimation()
  {
    if (this.speechLeftAnimation == null)
    {
      this.speechLeftAnimation = new TranslateAnimation(150.0F, -150.0F, 0.0F, 0.0F);
      this.speechLeftAnimation.setDuration(1000L);
      this.speechLeftAnimation.setStartOffset(0L);
      this.speechLeftAnimation.setFillAfter(false);
      this.speechLeftAnimation.setRepeatCount(-1);
    }
    for (;;)
    {
      this.speechPointImageView.setBackgroundResource(2130837651);
      this.speechPointImageView.startAnimation(this.speechLeftAnimation);
      return;
      this.speechLeftAnimation.reset();
    }
  }
  
  private void startSpeechListeningAnimation()
  {
    if (this.speechListeningAnimation == null)
    {
      this.speechListeningAnimation = ((AnimationDrawable)getResources().getDrawable(2130968589));
      this.speechListeningAnimation.setOneShot(false);
    }
    this.speechInputImageView.setBackground(this.speechListeningAnimation);
    this.speechListeningAnimation.start();
  }
  
  private void startSpeechRightPointAnimation()
  {
    if (this.speechRightAnimation == null)
    {
      this.speechRightAnimation = new TranslateAnimation(-150.0F, 150.0F, 0.0F, 0.0F);
      this.speechRightAnimation.setDuration(1000L);
      this.speechRightAnimation.setStartOffset(0L);
      this.speechRightAnimation.setFillAfter(false);
      this.speechRightAnimation.setRepeatCount(-1);
    }
    for (;;)
    {
      this.speechPointImageView.setBackgroundResource(2130837652);
      this.speechPointImageView.startAnimation(this.speechRightAnimation);
      return;
      this.speechRightAnimation.reset();
    }
  }
  
  private void startSpeechUpHeadPointAnimation()
  {
    this.speechPointImageView.setBackgroundResource(2130968591);
    this.speechPointAnimation = ((AnimationDrawable)this.speechPointImageView.getBackground());
    this.speechPointAnimation.setOneShot(false);
    this.speechPointAnimation.start();
  }
  
  private void stopAutoCharge()
  {
    isChargeSearching = false;
    if (this.beginChargeButton.getVisibility() != 0) {
      this.beginChargeButton.setVisibility(0);
    }
    if (this.endChargeButton.getVisibility() == 0) {
      this.endChargeButton.setVisibility(4);
    }
    stopChargingAnimation();
    stopSearchChargeAnimation();
    RobotVo localRobotVo = DataContainer.getDataContainer().getCurrentRobotVo();
    if (isChargeing)
    {
      isChargeing = false;
      BluetoothService.getInstance().sendInstruction(localRobotVo, "%");
    }
    while (isChargeFinding)
    {
      findChargeBase();
      return;
      BluetoothService.getInstance().sendInstruction(localRobotVo, ">");
    }
    findNoChargeBase();
    if (this.unChargeHandler != null)
    {
      this.unChargeHandler.removeCallbacks(this.unChargeRunable);
      this.unChargeHandler = null;
    }
    this.unChargeHandler = new Handler();
    this.unChargeHandler.postDelayed(this.unChargeRunable, 10000L);
  }
  
  private void stopChargingAnimation()
  {
    if ((this.chargingAnimation != null) && (this.chargingAnimation.isRunning())) {
      this.chargingAnimation.stop();
    }
  }
  
  private void stopSpeechControlMoveWithoutShowPoint()
  {
    sendRobotOrder("0");
    stopSpeechPointAnimation();
    if (this.speechPointImageView.getVisibility() == 0) {
      this.speechPointImageView.setVisibility(8);
    }
    this.robotLeftControlView.stopAutoMove();
    this.robotRightControlView.stopAutoMove();
  }
  
  private void stopSpeechListeningAnimation()
  {
    if (this.speechListeningAnimation != null) {
      this.speechListeningAnimation.stop();
    }
    this.speechInputImageView.setBackground(null);
  }
  
  private void stopSpeechPointAnimation()
  {
    if (this.speechPointAnimation != null)
    {
      this.speechPointAnimation.stop();
      this.speechPointAnimation = null;
    }
    if (this.speechForwardAnimation != null) {
      this.speechForwardAnimation.cancel();
    }
    if (this.speechBackwardAnimation != null) {
      this.speechBackwardAnimation.cancel();
    }
    if (this.speechLeftAnimation != null) {
      this.speechLeftAnimation.cancel();
    }
    if (this.speechRightAnimation != null) {
      this.speechRightAnimation.cancel();
    }
    this.speechPointImageView.clearAnimation();
  }
  
  public void beginSpeechInput()
  {
    this.speechOffLineRecognizeService.startRecognize(true);
  }
  
  public void buildGrammarError(String paramString)
  {
    this.speechImageView.setVisibility(8);
    this.speechPointImageView.setVisibility(8);
    this.speechInputImageView.setVisibility(8);
  }
  
  public void buildGrammarSuccesss() {}
  
  public void findChargeBase()
  {
    if (this.chargeImageView.getVisibility() != 0) {
      this.chargeImageView.setVisibility(0);
    }
    if (this.notChargeImageView.getVisibility() == 0) {
      this.notChargeImageView.setVisibility(4);
    }
    if (this.robotFirstImageView.getVisibility() != 0) {
      this.robotFirstImageView.setVisibility(0);
    }
    if (this.robotThirdImageView.getVisibility() == 0) {
      this.robotThirdImageView.setVisibility(4);
    }
    if (this.cartoonBalteryImageView.getVisibility() == 0) {
      this.cartoonBalteryImageView.setVisibility(4);
    }
  }
  
  public void findNoChargeBase()
  {
    if (this.chargeImageView.getVisibility() == 0) {
      this.chargeImageView.setVisibility(4);
    }
    if (this.notChargeImageView.getVisibility() != 0) {
      this.notChargeImageView.setVisibility(0);
    }
    if (this.robotFirstImageView.getVisibility() != 0) {
      this.robotFirstImageView.setVisibility(0);
    }
    if (this.robotThirdImageView.getVisibility() == 0) {
      this.robotThirdImageView.setVisibility(4);
    }
    if (this.cartoonBalteryImageView.getVisibility() == 0) {
      this.cartoonBalteryImageView.setVisibility(4);
    }
  }
  
  public void finishSpeechInput()
  {
    this.speechOffLineRecognizeService.stopRecognize();
  }
  
  public void gestureRecognizerOrder(int paramInt)
  {
    if ((1 == paramInt) && (this.lastGestureOrder != paramInt)) {
      sendRobotOrder("X5");
    }
    for (;;)
    {
      this.lastGestureOrder = paramInt;
      return;
      if ((4 == paramInt) && (this.lastGestureOrder != paramInt)) {
        sendRobotOrder("XA");
      } else if ((2 == paramInt) && (this.lastGestureOrder != paramInt)) {
        sendRobotOrder("XB");
      } else if ((3 == paramInt) && (this.lastGestureOrder != paramInt)) {
        sendRobotOrder("XC");
      } else {
        sendRobotOrder("0");
      }
    }
  }
  
  public void hideFindChargeBase()
  {
    if (this.chargeImageView.getVisibility() == 0) {
      this.chargeImageView.setVisibility(4);
    }
    if (this.notChargeImageView.getVisibility() == 0) {
      this.notChargeImageView.setVisibility(4);
    }
  }
  
  public void initOperatePoint()
  {
    int i;
    if (this.pointViewPager == null)
    {
      localObject1 = getLayoutInflater();
      this.pointPageViewList = new ArrayList();
      localObject2 = ((LayoutInflater)localObject1).inflate(2130903096, null);
      this.pointPageViewList.add(localObject2);
      this.pointHandModeImageView = ((ImageView)((View)localObject2).findViewById(2131362255));
      if (this.isUseSpeechFunction)
      {
        localObject1 = ((LayoutInflater)localObject1).inflate(2130903097, null);
        this.pointPageViewList.add(localObject1);
        this.poinSpeechModeImageView = ((ImageView)((View)localObject1).findViewById(2131362256));
      }
      this.pointViewPager = ((ViewPager)findViewById(2131362114));
      this.pointViewPager.setAdapter(new GuidePageAdapter(null));
      this.pointViewPager.setOnPageChangeListener(new GuidePageChangeListener());
      this.pointCursorImageViews = new ImageView[this.pointPageViewList.size()];
      if (this.isUseSpeechFunction)
      {
        this.pointGroupLayout = ((LinearLayout)findViewById(2131362115));
        setupRelativeLayoutMargin(this.pointGroupLayout, 0, 130, 0, 0);
        i = 0;
        if (i < this.pointPageViewList.size()) {
          break label214;
        }
      }
    }
    setupOperatePointImage();
    return;
    label214:
    Object localObject1 = new ImageView(this);
    Object localObject2 = new LinearLayout.LayoutParams(25, 25);
    ((LinearLayout.LayoutParams)localObject2).setMargins(15, 0, 15, 0);
    ((ImageView)localObject1).setLayoutParams((ViewGroup.LayoutParams)localObject2);
    this.pointCursorImageViews[i] = localObject1;
    if (i == 0) {
      this.pointCursorImageViews[i].setBackgroundResource(2130837633);
    }
    for (;;)
    {
      this.pointGroupLayout.addView(this.pointCursorImageViews[i]);
      i += 1;
      break;
      this.pointCursorImageViews[i].setBackgroundResource(2130837632);
    }
  }
  
  public void notifyAutoCharge(final int paramInt)
  {
    isChargeing = false;
    new Thread()
    {
      public void run()
      {
        Looper.prepare();
        new Handler().postDelayed(new Runnable()
        {
          public void run()
          {
            Message localMessage = new Message();
            localMessage.what = this.val$value;
            PHRobotControlActivity.this.chargeLayoutHandle.sendMessage(localMessage);
          }
        }, 1L);
        Looper.loop();
      }
    }.start();
    if ((isChargeing) || (isChargeSearching) || (!this.autoInfra)) {
      this.obstacleInfraLayout.setVisibility(8);
    }
  }
  
  public void notifyConnect() {}
  
  public void notifyDisconnect()
  {
    Message localMessage = new Message();
    this.notifyDisconnectHandler.sendMessage(localMessage);
  }
  
  public void notifyInfra(final String paramString)
  {
    boolean bool3 = false;
    boolean bool7 = false;
    boolean bool4 = false;
    boolean bool8 = false;
    boolean bool2 = false;
    boolean bool6 = false;
    boolean bool1 = false;
    boolean bool5 = false;
    if ((isChargeing) || (isChargeSearching))
    {
      this.obstacleFlag = 0;
      if (this.lastObstacleFlag != this.obstacleFlag)
      {
        paramString = new Message();
        paramString.what = this.obstacleFlag;
        this.obstacleHandle.sendMessage(paramString);
        this.lastObstacleFlag = this.obstacleFlag;
      }
      paramString = DataContainer.getDataContainer().getCurrentRobotVo();
      if (paramString.getRobotVersion() <= 1001)
      {
        if (this.robotInfraTimerTask == null) {
          this.robotInfraTimerTask = new TimerTask()
          {
            public void run()
            {
              BluetoothService.getInstance().sendInstruction(paramString, "&");
            }
          };
        }
        if (this.robotInfraTimer == null)
        {
          this.robotInfraTimer = new Timer();
          this.robotInfraTimer.schedule(this.robotInfraTimerTask, 100L, 1000L);
        }
      }
    }
    RobotDefaultInfo localRobotDefaultInfo;
    do
    {
      return;
      localRobotDefaultInfo = DataContainer.getDataContainer().getRobotDefaultInfo();
    } while (localRobotDefaultInfo == null);
    paramString = paramString.split(",");
    int i;
    if (paramString.length > 0)
    {
      i = 0;
      bool4 = bool8;
      bool3 = bool7;
      bool2 = bool6;
      bool1 = bool5;
    }
    for (;;)
    {
      if (i >= paramString.length)
      {
        if ((bool3) || (bool4) || (bool2) || (bool1)) {
          break label641;
        }
        this.obstacleFlag = 0;
        break;
      }
      boolean bool9 = bool1;
      boolean bool10 = bool2;
      boolean bool11 = bool3;
      boolean bool12 = bool4;
      try
      {
        int j = Integer.valueOf(paramString[i]).intValue();
        if (i == 0)
        {
          bool9 = bool1;
          bool10 = bool2;
          bool11 = bool3;
          bool12 = bool4;
          if (j < localRobotDefaultInfo.getLeftFrontInfra())
          {
            bool7 = true;
            bool8 = bool4;
            bool6 = bool2;
            bool5 = bool1;
          }
        }
        for (;;)
        {
          bool9 = bool5;
          bool10 = bool6;
          bool11 = bool7;
          bool12 = bool8;
          bool1 = bool5;
          bool2 = bool6;
          bool3 = bool7;
          bool4 = bool8;
          if (i != paramString.length - 1) {
            break;
          }
          bool9 = bool5;
          bool10 = bool6;
          bool11 = bool7;
          bool12 = bool8;
          Log.i("robotControl", "障碍物 left " + bool7 + " ,right " + bool8 + ", front " + bool6 + ", back " + bool5);
          bool1 = bool5;
          bool2 = bool6;
          bool3 = bool7;
          bool4 = bool8;
          break;
          if (i == 1)
          {
            bool9 = bool1;
            bool10 = bool2;
            bool11 = bool3;
            bool12 = bool4;
            if (j > localRobotDefaultInfo.getFrontInfra())
            {
              bool6 = true;
              bool5 = bool1;
              bool7 = bool3;
              bool8 = bool4;
              continue;
            }
          }
          if (i == 2)
          {
            bool9 = bool1;
            bool10 = bool2;
            bool11 = bool3;
            bool12 = bool4;
            if (j < localRobotDefaultInfo.getRightFrontInfra())
            {
              bool8 = true;
              bool5 = bool1;
              bool6 = bool2;
              bool7 = bool3;
              continue;
            }
          }
          bool5 = bool1;
          bool6 = bool2;
          bool7 = bool3;
          bool8 = bool4;
          if (i == 3)
          {
            double d1 = j;
            bool9 = bool1;
            bool10 = bool2;
            bool11 = bool3;
            bool12 = bool4;
            double d2 = localRobotDefaultInfo.getBackInfra();
            bool5 = bool1;
            bool6 = bool2;
            bool7 = bool3;
            bool8 = bool4;
            if (d1 < d2)
            {
              bool5 = true;
              bool6 = bool2;
              bool7 = bool3;
              bool8 = bool4;
            }
          }
        }
        label641:
        if ((bool3) && (!bool4) && (!bool2) && (!bool1)) {
          this.obstacleFlag = 1;
        }
        if ((!bool3) && (bool4) && (!bool2) && (!bool1)) {
          this.obstacleFlag = 2;
        }
        if ((!bool3) && (!bool4) && (bool2) && (!bool1)) {
          this.obstacleFlag = 3;
        }
        if ((!bool3) && (!bool4) && (!bool2) && (bool1)) {
          this.obstacleFlag = 4;
        }
        if ((bool3) && (bool4) && (!bool2) && (!bool1)) {
          this.obstacleFlag = 5;
        }
        if ((bool3) && (bool4) && (bool2) && (!bool1)) {
          this.obstacleFlag = 3;
        }
        if ((bool3) && (bool4) && (!bool2) && (bool1)) {
          this.obstacleFlag = 7;
        }
        if ((bool3) && (!bool4) && (bool2) && (!bool1)) {
          this.obstacleFlag = 3;
        }
        if ((!bool3) && (bool4) && (bool2) && (!bool1)) {
          this.obstacleFlag = 3;
        }
        if ((bool3) && (!bool4) && (!bool2) && (bool1)) {
          this.obstacleFlag = 10;
        }
        if ((!bool3) && (bool4) && (!bool2) && (bool1)) {
          this.obstacleFlag = 11;
        }
        if ((!bool3) || (!bool4) || (!bool2) || (!bool1)) {
          break;
        }
        this.obstacleFlag = 3;
      }
      catch (Exception localException)
      {
        bool4 = bool12;
        bool3 = bool11;
        bool2 = bool10;
        bool1 = bool9;
        i += 1;
      }
    }
  }
  
  public void notifyVersion(int paramInt)
  {
    Message localMessage = new Message();
    localMessage.what = paramInt;
    this.notifyVersionHandler.sendMessage(localMessage);
  }
  
  public void notifyVoltage(int paramInt)
  {
    Object localObject = DataContainer.getDataContainer().getCurrentRobotVo();
    if ((localObject != null) && (((RobotVo)localObject).getRobotVersion() > 1001))
    {
      double d = paramInt / 25.0D;
      localObject = new Message();
      Bundle localBundle = new Bundle();
      localBundle.putInt("voltage", paramInt);
      localBundle.putDouble("volFlag", d);
      ((Message)localObject).setData(localBundle);
      this.powerHandler.sendMessage((Message)localObject);
    }
  }
  
  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    try
    {
      super.onConfigurationChanged(paramConfiguration);
      setupOperatePointImage();
      return;
    }
    catch (Exception paramConfiguration) {}
  }
  
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    requestWindowFeature(1);
    setContentView(2130903070);
    this.robotLeftControlView = ((RobotLeftControlView)findViewById(2131362097));
    this.robotLeftControlView.setZOrderOnTop(true);
    this.robotLeftControlView.getHolder().setFormat(-3);
    this.robotLeftControlView.setRobotLeftControlListener(new RobotLeftControlView.RobotLeftControlListener()
    {
      public void sendOrder(int paramAnonymousInt)
      {
        if (PHRobotControlActivity.this.lastLeftOrder != paramAnonymousInt)
        {
          PHRobotControlActivity.this.sendOrderToRobot(paramAnonymousInt, PHRobotControlActivity.this.lastRightOrder);
          PHRobotControlActivity.this.lastLeftOrder = paramAnonymousInt;
        }
      }
      
      public void touchBegin()
      {
        PHRobotControlActivity.this.isInTouchControl = true;
        if (PHRobotControlActivity.this.isInSpeechControl)
        {
          PHRobotControlActivity.this.isInSpeechControl = false;
          PHRobotControlActivity.this.stopSpeechControlMoveWithoutShowPoint();
        }
      }
      
      public void touchEnd()
      {
        if ((!PHRobotControlActivity.this.robotLeftControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotRightControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotGestureControlView.getIsInTouch())) {
          PHRobotControlActivity.this.isInTouchControl = false;
        }
      }
    });
    this.robotRightControlView = ((RobotRightControlView)findViewById(2131362098));
    this.robotRightControlView.setZOrderOnTop(true);
    this.robotRightControlView.getHolder().setFormat(-3);
    this.robotRightControlView.setRobotRightControlListener(new RobotRightControlView.RobotRightControlListener()
    {
      public void sendOrder(int paramAnonymousInt)
      {
        if (PHRobotControlActivity.this.lastRightOrder != paramAnonymousInt)
        {
          PHRobotControlActivity.this.sendOrderToRobot(PHRobotControlActivity.this.lastLeftOrder, paramAnonymousInt);
          PHRobotControlActivity.this.lastRightOrder = paramAnonymousInt;
        }
      }
      
      public void touchBegin()
      {
        PHRobotControlActivity.this.isInTouchControl = true;
        if (PHRobotControlActivity.this.isInSpeechControl)
        {
          PHRobotControlActivity.this.isInSpeechControl = false;
          PHRobotControlActivity.this.stopSpeechControlMoveWithoutShowPoint();
        }
      }
      
      public void touchEnd()
      {
        if ((!PHRobotControlActivity.this.robotLeftControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotRightControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotGestureControlView.getIsInTouch())) {
          PHRobotControlActivity.this.isInTouchControl = false;
        }
      }
    });
    this.robotGestureControlView = ((RobotGestureControlView)findViewById(2131362094));
    this.robotGestureControlView.setRobotGestureControlListener(new RobotGestureControlView.RobotGestureControlListener()
    {
      public void sendGestureOrder(int paramAnonymousInt)
      {
        if (PHRobotControlActivity.this.lastGestureOrder != paramAnonymousInt) {
          PHRobotControlActivity.this.gestureRecognizerOrder(paramAnonymousInt);
        }
      }
      
      public void touchBegin()
      {
        PHRobotControlActivity.this.isInTouchControl = true;
        if (PHRobotControlActivity.this.isInSpeechControl)
        {
          PHRobotControlActivity.this.isInSpeechControl = false;
          PHRobotControlActivity.this.stopSpeechControlMoveWithoutShowPoint();
        }
      }
      
      public void touchEnd()
      {
        if ((!PHRobotControlActivity.this.robotLeftControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotRightControlView.getIsInTouch()) && (!PHRobotControlActivity.this.robotGestureControlView.getIsInTouch())) {
          PHRobotControlActivity.this.isInTouchControl = false;
        }
      }
      
      public void touchTab() {}
    });
    paramBundle = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(paramBundle);
    this.currenrname = DataContainer.getDataContainer().getCurrentUsername();
    if (StringUtils.isEmpty(LocalDataService.getInstance().getLocalStringData(this, this.currenrname, "FIRST_ENTER_ROBOT_CONTROL")))
    {
      this.robotLeftControlView.hide();
      this.robotRightControlView.hide();
      new Handler().postDelayed(new Runnable()
      {
        public void run()
        {
          PHRobotControlActivity.this.setPointViewInvisible();
        }
      }, 3000L);
      LocalDataService.getInstance().saveLocalData(this, this.currenrname, "FIRST_ENTER_ROBOT_CONTROL", "YES");
      paramBundle = (ImageView)findViewById(2131362083);
      setupRelativeLayoutMargin(2131362083, 40, 32, 0, 0);
      setupRelativeLayoutParams(2131362083, 66, 66);
      paramBundle.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.sendRobotOrder("0");
          PHRobotControlActivity.this.finish();
          PHRobotControlActivity.this.overridePendingTransition(2130968581, 2130968584);
        }
      });
      setupRelativeLayoutMargin(2131362089, 0, 32, 10, 0);
      this.chargeButtonImageView = ((ImageView)findViewById(2131362091));
      setupLinearLayoutMargin(2131362091, 0, 0, 30, 0);
      setupLinearLayoutParams(2131362091, 66, 66);
      if (this.chargeButtonImageView.getVisibility() == 0) {
        this.chargeButtonImageView.setVisibility(8);
      }
      this.chargeButtonImageView.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.chargeButtonImageView.setVisibility(8);
          PHRobotControlActivity.this.robotChargeLayout.setVisibility(0);
          if (PHRobotControlActivity.isChargeFinding) {
            PHRobotControlActivity.this.findChargeBase();
          }
          for (;;)
          {
            if (PHRobotControlActivity.this.unChargeHandler != null)
            {
              PHRobotControlActivity.this.unChargeHandler.removeCallbacks(PHRobotControlActivity.this.unChargeRunable);
              PHRobotControlActivity.this.unChargeHandler = null;
            }
            PHRobotControlActivity.this.unChargeHandler = new Handler();
            PHRobotControlActivity.this.unChargeHandler.postDelayed(PHRobotControlActivity.this.unChargeRunable, 10000L);
            return;
            PHRobotControlActivity.this.findNoChargeBase();
          }
        }
      });
      paramBundle = (ImageView)findViewById(2131362092);
      setupLinearLayoutMargin(2131362092, 0, 0, 30, 0);
      setupLinearLayoutParams(2131362092, 66, 66);
      paramBundle.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.setPointViewVisible();
        }
      });
      this.configImageView = ((ImageView)findViewById(2131362093));
      setupLinearLayoutMargin(2131362093, 0, 0, 30, 0);
      setupLinearLayoutParams(2131362093, 66, 66);
      paramBundle = DataContainer.getDataContainer().getRobotConfigVo();
      this.autoInfra = true;
      if (paramBundle == null) {
        break label1658;
      }
      this.autoInfra = paramBundle.getAutoInfra();
      if (!paramBundle.getAutoInfra()) {
        break label1651;
      }
      paramBundle = "Y";
      label502:
      sendRobotOrder(paramBundle);
      this.configImageView.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          paramAnonymousView = DataContainer.getDataContainer().getCurrentRobotVo();
          if ((paramAnonymousView == null) || (StringUtils.isEmpty(paramAnonymousView.getRobotName())))
          {
            ToastUtils.show(PHRobotControlActivity.this, 2131099987);
            return;
          }
          paramAnonymousView = new Intent();
          paramAnonymousView.setClass(PHRobotControlActivity.this, PHRobotConfigActivity.class);
          PHRobotControlActivity.this.startActivity(paramAnonymousView);
          PHRobotControlActivity.this.overridePendingTransition(2130968583, 2130968582);
        }
      });
      this.robotChargeLayoutWidth = 180;
      this.obstacleInfraLayout = ((RelativeLayout)findViewById(2131362101));
      this.obstacleInfraLayout.getBackground().setAlpha(204);
      setupLinearLayoutParams(2131362101, 180, this.robotChargeLayoutWidth);
      this.obstacleInfraImageView = ((ImageView)findViewById(2131362102));
      this.obstacleInfraLayout.setVisibility(8);
      this.robotChargeLayout = ((RelativeLayout)findViewById(2131362103));
      this.robotChargeLayout.getBackground().setAlpha(204);
      setupLinearLayoutParams(2131362103, 180, this.robotChargeLayoutWidth);
      this.robotChargeLayout.setVisibility(8);
      this.robotFirstImageView = ((ImageView)findViewById(2131362109));
      this.robotThirdImageView = ((ImageView)findViewById(2131362110));
      this.robotSeatImageView = ((ImageView)findViewById(2131362108));
      this.chargeImageView = ((ImageView)findViewById(2131362111));
      this.notChargeImageView = ((ImageView)findViewById(2131362112));
      this.cartoonBalteryImageView = ((ImageView)findViewById(2131362113));
      this.beginChargeButton = ((Button)findViewById(2131362105));
      this.endChargeButton = ((Button)findViewById(2131362106));
      setupLinearLayoutMargin(2131362101, 0, 20, 40, 0);
      setupLinearLayoutMargin(2131362103, 0, 20, 40, 0);
      this.robotViewWidth = 36;
      this.robotMarginLeftRight = ((this.robotChargeLayoutWidth - this.robotViewWidth * 3) / 4);
      int i = 36 / 2;
      setupRelativeLayoutParams(2131362109, 94, this.robotViewWidth);
      setupRelativeLayoutParams(2131362110, 94, this.robotViewWidth);
      setupRelativeLayoutParams(2131362108, 45, 54);
      setupRelativeLayoutParams(2131362113, 57, 38);
      setupRelativeLayoutParams(2131362111, 68, 30);
      setupRelativeLayoutParams(2131362112, 38, 38);
      setupRelativeLayoutMargin(2131362107, 0, i, 0, i);
      setupRelativeLayoutMargin(2131362109, this.robotMarginLeftRight, 0, 0, 0);
      setupRelativeLayoutMargin(2131362110, 0, 0, this.robotMarginLeftRight, 0);
      setupRelativeLayoutMargin(2131362113, this.robotChargeLayoutWidth / 3 - 19, 0, 0, 0);
      setupRelativeLayoutMargin(2131362104, 10, 0, 10, 10);
      setupRelativeLayoutMargin(2131362108, 0, 0, this.robotMarginLeftRight - 9, 10);
      setupViewPadding(this.robotSeatImageView, 0, 0, 0, 10);
      setupRelativeLayoutParams(2131362105, 40, 0);
      setupRelativeLayoutParams(2131362106, 40, 0);
      setupButtonFontSize(2131362105, 12);
      setupButtonFontSize(2131362106, 12);
      this.robotThirdImageView.setVisibility(4);
      this.beginChargeButton.setVisibility(0);
      this.endChargeButton.setVisibility(4);
      this.beginChargeButton.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.startAutoCharge();
        }
      });
      this.endChargeButton.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.stopAutoCharge();
        }
      });
      if (DataContainer.getDataContainer().getCurrentRobotVo() == null) {
        ToastUtils.show(getApplicationContext(), 2131099830);
      }
      if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth_le"))
      {
        paramBundle = new HashMap();
        paramBundle.put("username", DataContainer.getDataContainer().getCurrentUsername());
        paramBundle.put("deviceType", Build.MODEL);
        TCAgent.onEvent(this, "bluetoothError", "", paramBundle);
        if (!Build.MODEL.equals("Nexus 7")) {
          break label1665;
        }
        showPromptInfoAlert(getString(2131099965));
        label1178:
        TalkingDataService.getInstance().recordBluetoothBleError(this);
      }
      if ((BluetoothService.getInstance().mBluetoothAdapter == null) || (!BluetoothService.getInstance().mBluetoothAdapter.isEnabled())) {
        startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 1);
      }
      this.chargeLayoutHandle = new Handler(getMainLooper())
      {
        public void handleMessage(Message paramAnonymousMessage)
        {
          if (1 == paramAnonymousMessage.what)
          {
            if (PHRobotControlActivity.this.unChargeHandler != null)
            {
              PHRobotControlActivity.this.unChargeHandler.removeCallbacks(PHRobotControlActivity.this.unChargeRunable);
              PHRobotControlActivity.this.unChargeHandler = null;
            }
            PHRobotControlActivity.isChargeFinding = true;
            PHRobotControlActivity.isChargeing = false;
            PHRobotControlActivity.isChargeSearching = false;
            if (PHRobotControlActivity.this.beginChargeButton.getVisibility() != 0) {
              PHRobotControlActivity.this.beginChargeButton.setVisibility(0);
            }
            if (PHRobotControlActivity.this.endChargeButton.getVisibility() == 0) {
              PHRobotControlActivity.this.endChargeButton.setVisibility(4);
            }
            PHRobotControlActivity.this.stopChargingAnimation();
            PHRobotControlActivity.this.stopSearchChargeAnimation();
            PHRobotControlActivity.this.findChargeBase();
          }
          for (;;)
          {
            if (((PHRobotControlActivity.isChargeing) || (PHRobotControlActivity.isChargeSearching) || (!PHRobotControlActivity.this.autoInfra)) && (PHRobotControlActivity.this.obstacleInfraLayout.getVisibility() == 0)) {
              PHRobotControlActivity.this.obstacleInfraLayout.setVisibility(8);
            }
            if (PHRobotControlActivity.this.robotChargeLayout.getVisibility() != 0) {
              PHRobotControlActivity.this.robotChargeLayout.setVisibility(0);
            }
            if (PHRobotControlActivity.this.chargeButtonImageView.getVisibility() == 0) {
              PHRobotControlActivity.this.chargeButtonImageView.setVisibility(8);
            }
            if (PHRobotControlActivity.this.powerChargeImageView.getVisibility() == 0) {
              PHRobotControlActivity.this.powerChargeImageView.setVisibility(4);
            }
            return;
            if (2 == paramAnonymousMessage.what)
            {
              PHRobotControlActivity.isChargeSearching = false;
              PHRobotControlActivity.isChargeing = true;
              PHRobotControlActivity.isChargeFinding = true;
              if (PHRobotControlActivity.this.powerChargeImageView.getVisibility() != 0) {
                PHRobotControlActivity.this.powerChargeImageView.setVisibility(0);
              }
              if (PHRobotControlActivity.this.beginChargeButton.getVisibility() == 0) {
                PHRobotControlActivity.this.beginChargeButton.setVisibility(4);
              }
              if (PHRobotControlActivity.this.endChargeButton.getVisibility() != 0) {
                PHRobotControlActivity.this.endChargeButton.setVisibility(0);
              }
              PHRobotControlActivity.this.hideFindChargeBase();
              PHRobotControlActivity.this.stopSearchChargeAnimation();
              PHRobotControlActivity.this.startChargingAnimation();
            }
            else if (3 == paramAnonymousMessage.what)
            {
              PHRobotControlActivity.isChargeSearching = true;
              PHRobotControlActivity.isChargeing = false;
              if (PHRobotControlActivity.this.beginChargeButton.getVisibility() == 0) {
                PHRobotControlActivity.this.beginChargeButton.setVisibility(4);
              }
              if (PHRobotControlActivity.this.endChargeButton.getVisibility() != 0) {
                PHRobotControlActivity.this.endChargeButton.setVisibility(0);
              }
              PHRobotControlActivity.this.hideFindChargeBase();
              PHRobotControlActivity.this.stopChargingAnimation();
              PHRobotControlActivity.this.startSearchChargeAnimation();
            }
            else
            {
              PHRobotControlActivity.isChargeSearching = false;
              PHRobotControlActivity.isChargeing = false;
              PHRobotControlActivity.isChargeFinding = false;
              if (PHRobotControlActivity.this.unChargeHandler != null)
              {
                PHRobotControlActivity.this.unChargeHandler.removeCallbacks(PHRobotControlActivity.this.unChargeRunable);
                PHRobotControlActivity.this.unChargeHandler = null;
              }
              PHRobotControlActivity.this.unChargeHandler = new Handler();
              PHRobotControlActivity.this.unChargeHandler.postAtTime(PHRobotControlActivity.this.unChargeRunable, 10000L);
              if (PHRobotControlActivity.this.beginChargeButton.getVisibility() != 0) {
                PHRobotControlActivity.this.beginChargeButton.setVisibility(0);
              }
              if (PHRobotControlActivity.this.endChargeButton.getVisibility() == 0) {
                PHRobotControlActivity.this.endChargeButton.setVisibility(4);
              }
              PHRobotControlActivity.this.findNoChargeBase();
              PHRobotControlActivity.this.stopChargingAnimation();
              PHRobotControlActivity.this.stopSearchChargeAnimation();
            }
          }
        }
      };
      this.powerLayout = ((LinearLayout)findViewById(2131362084));
      this.powerLayout.setVisibility(4);
      this.powerLayout.setMinimumWidth(90);
      this.powerLayout.setMinimumHeight(50);
      setupRelativeLayoutMargin(this.powerLayout, 40, 20, 0, 0);
      setupLinearLayoutMargin(2131362085, 10, 10, 0, 10);
      this.powerImageView = ((ImageView)findViewById(2131362086));
      setupRelativeLayoutParams(2131362086, 30, 53);
      setupRelativeLayoutParams(2131362087, 14, 9);
      this.powerChargeImageView = ((ImageView)findViewById(2131362087));
      this.powerValueTextView = ((TextView)findViewById(2131362088));
      setupLinearLayoutMargin(2131362088, 5, 10, 10, 10);
      setupTextViewFontSize(this.powerValueTextView, 14);
      this.powerChargeImageView.setVisibility(4);
      if (!LocalUtils.isCurrentLanguageSimplifiedChinese()) {
        break label1702;
      }
    }
    label1651:
    label1658:
    label1665:
    label1702:
    for (this.isUseSpeechFunction = true;; this.isUseSpeechFunction = false)
    {
      if (this.isUseSpeechFunction)
      {
        this.speechImageView = ((ImageView)findViewById(2131362090));
        this.speechImageView.setVisibility(0);
        setupLinearLayoutMargin(this.speechImageView, 0, 0, 30, 0);
        setupLinearLayoutParams(this.speechImageView, 66, 66);
        this.speechImageView.setOnClickListener(new View.OnClickListener()
        {
          public void onClick(View paramAnonymousView)
          {
            if (!PHRobotControlActivity.this.isSpeechControlEnable)
            {
              paramAnonymousView = DataContainer.getDataContainer().getCurrentRobotVo();
              if ((paramAnonymousView == null) || (StringUtils.isEmpty(paramAnonymousView.getRobotName())))
              {
                ToastUtils.show(PHRobotControlActivity.this, 2131099987);
                return;
              }
              PHRobotControlActivity.this.isSpeechControlEnable = true;
              PHRobotControlActivity.this.speechImageView.setImageResource(2130837637);
              PHRobotControlActivity.this.robotLeftControlView.hide();
              PHRobotControlActivity.this.robotRightControlView.hide();
              PHRobotControlActivity.this.touchControlLayout.setVisibility(8);
              PHRobotControlActivity.this.speechControlLayout.setVisibility(0);
              return;
            }
            PHRobotControlActivity.this.isSpeechControlEnable = false;
            PHRobotControlActivity.this.speechImageView.setImageResource(2130837638);
            PHRobotControlActivity.this.robotLeftControlView.show();
            PHRobotControlActivity.this.robotRightControlView.show();
            PHRobotControlActivity.this.touchControlLayout.setVisibility(0);
            PHRobotControlActivity.this.speechControlLayout.setVisibility(8);
          }
        });
        this.touchControlLayout = ((RelativeLayout)findViewById(2131362096));
        this.speechControlLayout = ((RelativeLayout)findViewById(2131362099));
        setupLinearLayoutParams(this.speechControlLayout, 280, 0);
        setupViewPadding(this.speechControlLayout, 0, 0, 0, 40);
        this.speechControlLayout.setVisibility(8);
        this.speechInputImageView = ((ImageView)findViewById(2131362100));
        setupRelativeLayoutParams(this.speechInputImageView, 200, 200);
        setupRelativeLayoutMargin(this.speechInputImageView, 0, 10, 0, 0);
        setupViewPadding(this.speechInputImageView, 40, 40, 40, 40);
        this.speechInputImageView.setOnTouchListener(new View.OnTouchListener()
        {
          public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent)
          {
            if (paramAnonymousMotionEvent.getAction() == 0) {
              PHRobotControlActivity.this.beginSpeechInput();
            }
            for (;;)
            {
              return true;
              if (paramAnonymousMotionEvent.getAction() == 3) {
                PHRobotControlActivity.this.finishSpeechInput();
              } else {
                paramAnonymousMotionEvent.getAction();
              }
            }
          }
        });
        this.speechPointImageView = ((ImageView)findViewById(2131362116));
        setupRelativeLayoutParams(this.speechPointImageView, 400, 400);
      }
      return;
      setPointViewInvisible();
      break;
      paramBundle = "Z";
      break label502;
      paramBundle = "Z";
      break label502;
      if (Build.MODEL.equals("Nexus 10"))
      {
        showPromptInfoAlert(getString(2131099966));
        break label1178;
      }
      ToastUtils.show(this, 2131099964);
      break label1178;
    }
  }
  
  protected void onPause()
  {
    this.padbotApp.setHandleRobotNotifyInterface(null);
    super.onPause();
  }
  
  protected void onResume()
  {
    super.onResume();
    this.padbotApp.setHandleRobotNotifyInterface(this);
    this.lastObstacleFlag = -1;
    this.obstacleInfraLayout.setVisibility(8);
    final RobotVo localRobotVo = DataContainer.getDataContainer().getCurrentRobotVo();
    if (localRobotVo != null)
    {
      this.autoInfra = DataContainer.getDataContainer().getRobotConfigVo().getAutoInfra();
      BluetoothService.getInstance().sendInstruction(localRobotVo, ";");
      new Timer().schedule(new TimerTask()
      {
        public void run()
        {
          BluetoothService.getInstance().sendInstruction(localRobotVo, ":");
        }
      }, 150L);
      if (localRobotVo.getRobotVersion() > 1001) {
        new Timer().schedule(new TimerTask()
        {
          public void run()
          {
            BluetoothService.getInstance().sendInstruction(localRobotVo, "?");
          }
        }, 300L);
      }
      new Timer().schedule(new TimerTask()
      {
        public void run()
        {
          BluetoothService.getInstance().sendInstruction(localRobotVo, "&");
        }
      }, 450L);
      new Timer().schedule(new TimerTask()
      {
        public void run()
        {
          BluetoothService.getInstance().sendInstruction(localRobotVo, ":");
        }
      }, 600L);
      if (localRobotVo.getRobotVersion() > 1001) {
        new Timer().schedule(new TimerTask()
        {
          public void run()
          {
            BluetoothService.getInstance().sendInstruction(localRobotVo, "?");
          }
        }, 750L);
      }
      new Timer().schedule(new TimerTask()
      {
        public void run()
        {
          BluetoothService.getInstance().sendInstruction(localRobotVo, "&");
        }
      }, 900L);
    }
  }
  
  protected void onStart()
  {
    super.onStart();
    RobotVo localRobotVo = DataContainer.getDataContainer().getCurrentRobotVo();
    if ((localRobotVo != null) && (StringUtils.isNotEmpty(localRobotVo.getRobotName())))
    {
      if (localRobotVo.getRobotVersion() < 1001) {
        this.configImageView.setVisibility(8);
      }
      this.robotOrderExecuteBufferStr = new StringBuffer("");
      if (this.robotOrderExecuteTimerTask != null)
      {
        this.robotOrderExecuteTimerTask.cancel();
        this.robotOrderExecuteTimerTask = null;
      }
      if (this.robotOrderExecuteTimerTask == null)
      {
        this.robotOrderExecuteTimerTask = new TimerTask()
        {
          public void run()
          {
            PHRobotControlActivity.this.executeRobotOrder();
          }
        };
        if (this.robotOrderExecuteTimer == null) {
          this.robotOrderExecuteTimer = new Timer();
        }
        this.robotOrderExecuteTimer.schedule(this.robotOrderExecuteTimerTask, 0L, 50L);
      }
    }
    for (;;)
    {
      if (this.robotHeartBeatTask != null)
      {
        this.robotHeartBeatTask.cancel();
        this.robotHeartBeatTask = null;
      }
      this.robotHeartBeatTimer = new Timer();
      new Handler().postDelayed(new Runnable()
      {
        public void run()
        {
          PHRobotControlActivity.this.initSpeedLevel();
        }
      }, 1000L);
      if (this.isUseSpeechFunction)
      {
        this.speechControlStopTimer = new Timer();
        bindService(new Intent(this, SpeechRecognizeService.class), this.conn, 1);
      }
      return;
      this.obstacleInfraLayout.setVisibility(8);
      this.robotChargeLayout.setVisibility(8);
      this.powerLayout.setVisibility(8);
      this.chargeButtonImageView.setVisibility(8);
      this.configImageView.setVisibility(8);
    }
  }
  
  protected void onStop()
  {
    super.onStop();
    if (this.robotOrderExecuteTimerTask != null)
    {
      this.robotOrderExecuteTimerTask.cancel();
      this.robotOrderExecuteTimerTask = null;
    }
    if (this.robotHeartBeatTask != null)
    {
      this.robotHeartBeatTask.cancel();
      this.robotHeartBeatTask = null;
    }
    if (this.robotOrderExecuteTimer != null)
    {
      this.robotOrderExecuteTimer.cancel();
      this.robotOrderExecuteTimer = null;
    }
    if (this.robotHeartBeatTimer != null)
    {
      this.robotHeartBeatTimer.cancel();
      this.robotHeartBeatTimer = null;
    }
    if (this.robotInfraTimer != null)
    {
      this.robotInfraTimer.cancel();
      this.robotInfraTimer = null;
    }
    if (this.robotInfraTimerTask != null)
    {
      this.robotInfraTimerTask.cancel();
      this.robotInfraTimerTask = null;
    }
    if (this.stopOrderTime != null)
    {
      this.stopOrderTime.cancel();
      this.stopOrderTime = null;
    }
    if (this.stopOrderTimerTask != null)
    {
      this.stopOrderTimerTask.cancel();
      this.stopOrderTime = null;
    }
    this.robotOrderExecuteBufferStr = null;
    if (this.speechControlStopTimer != null)
    {
      this.speechControlStopTimer.cancel();
      this.speechControlStopTimer = null;
    }
    if (this.isUseSpeechFunction) {
      unbindService(this.conn);
    }
  }
  
  public void prepareSpeechControlStopOrder(int paramInt)
  {
    this.speechControlStopTimerTask = new TimerTask()
    {
      public void run()
      {
        PHRobotControlActivity.this.speechControlHandler.sendEmptyMessage(10);
      }
    };
    this.speechControlStopTimer.schedule(this.speechControlStopTimerTask, paramInt);
  }
  
  public void setObstacleInfraImageView(int paramInt)
  {
    if (1 == paramInt)
    {
      if (this.obstacleLeftdrawable_1 == null) {
        this.obstacleLeftdrawable_1 = getResources().getDrawable(2130837732);
      }
      this.obstacleInfraImageView.setImageDrawable(this.obstacleLeftdrawable_1);
    }
    do
    {
      return;
      if (2 == paramInt)
      {
        if (this.obstacleRightdrawable_2 == null) {
          this.obstacleRightdrawable_2 = getResources().getDrawable(2130837737);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleRightdrawable_2);
        return;
      }
      if (3 == paramInt)
      {
        if (this.obstacleMiddledrawable_3 == null) {
          this.obstacleMiddledrawable_3 = getResources().getDrawable(2130837736);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleMiddledrawable_3);
        return;
      }
      if (4 == paramInt)
      {
        if (this.obstacleBackdrawable_4 == null) {
          this.obstacleBackdrawable_4 = getResources().getDrawable(2130837731);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleBackdrawable_4);
        return;
      }
      if (5 == paramInt)
      {
        if (this.obstacleLeftRightdrawable_5 == null) {
          this.obstacleLeftRightdrawable_5 = getResources().getDrawable(2130837734);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleLeftRightdrawable_5);
        return;
      }
      if (7 == paramInt)
      {
        if (this.obstacleLeftRightBackdrawable_7 == null) {
          this.obstacleLeftRightBackdrawable_7 = getResources().getDrawable(2130837735);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleLeftRightBackdrawable_7);
        return;
      }
      if (10 == paramInt)
      {
        if (this.obstacleLeftBackdrawable_10 == null) {
          this.obstacleLeftBackdrawable_10 = getResources().getDrawable(2130837733);
        }
        this.obstacleInfraImageView.setImageDrawable(this.obstacleLeftBackdrawable_10);
        return;
      }
    } while (11 != paramInt);
    if (this.obstacleRightBackdrawable_11 == null) {
      this.obstacleRightBackdrawable_11 = getResources().getDrawable(2130837738);
    }
    this.obstacleInfraImageView.setImageDrawable(this.obstacleRightBackdrawable_11);
  }
  
  public void setPointViewInvisible()
  {
    if (!this.isSpeechControlEnable)
    {
      this.robotLeftControlView.show();
      this.robotRightControlView.show();
    }
    if (this.pointViewPager != null)
    {
      this.pointViewPager.setVisibility(8);
      if (this.pointGroupLayout != null) {
        this.pointGroupLayout.setVisibility(8);
      }
    }
  }
  
  public void setPointViewVisible()
  {
    this.robotLeftControlView.hide();
    this.robotRightControlView.hide();
    initOperatePoint();
    if (this.pointViewPager != null)
    {
      this.pointViewPager.setVisibility(0);
      if (this.pointGroupLayout != null) {
        this.pointGroupLayout.setVisibility(0);
      }
    }
  }
  
  public void setPowerImageView(double paramDouble)
  {
    if (3.0D <= paramDouble)
    {
      if (this.powerDrawable_4 == null) {
        this.powerDrawable_4 = getResources().getDrawable(2130837720);
      }
      this.powerImageView.setImageDrawable(this.powerDrawable_4);
    }
    do
    {
      return;
      if ((2.0D <= paramDouble) && (3.0D > paramDouble))
      {
        if (this.powerDrawable_3 == null) {
          this.powerDrawable_3 = getResources().getDrawable(2130837719);
        }
        this.powerImageView.setImageDrawable(this.powerDrawable_3);
        return;
      }
      if ((1.0D <= paramDouble) && (2.0D > paramDouble))
      {
        if (this.powerDrawable_2 == null) {
          this.powerDrawable_2 = getResources().getDrawable(2130837718);
        }
        this.powerImageView.setImageDrawable(this.powerDrawable_2);
        return;
      }
      if ((0.5D <= paramDouble) && (paramDouble < 1.0D))
      {
        if (this.powerDrawable_1 == null) {
          this.powerDrawable_1 = getResources().getDrawable(2130837717);
        }
        this.powerImageView.setImageDrawable(this.powerDrawable_1);
        return;
      }
    } while (0.5D <= paramDouble);
    if (this.powerDrawable_0 == null) {
      this.powerDrawable_0 = getResources().getDrawable(2130837716);
    }
    this.powerImageView.setImageDrawable(this.powerDrawable_0);
  }
  
  public void speechControlBack()
  {
    sendRobotOrder("X4");
    stopSpeechPointAnimation();
    startSpeechBackwardPointAnimation();
    this.robotLeftControlView.goBackAutoMove();
    this.robotRightControlView.stopAutoMove();
  }
  
  public void speechControlDownHead()
  {
    sendRobotOrder("XA");
    stopSpeechPointAnimation();
    startSpeechDownHeadPointAnimation();
  }
  
  public void speechControlForward()
  {
    sendRobotOrder("X1");
    stopSpeechPointAnimation();
    startSpeechForwardPointAnimation();
    this.robotLeftControlView.goForwardAutoMove();
    this.robotRightControlView.stopAutoMove();
  }
  
  public void speechControlLeft()
  {
    sendRobotOrder("X2");
    stopSpeechPointAnimation();
    startSpeechLeftPointAnimation();
    this.robotLeftControlView.stopAutoMove();
    this.robotRightControlView.turnLeftAutoMove();
  }
  
  public void speechControlRight()
  {
    sendRobotOrder("X3");
    stopSpeechPointAnimation();
    startSpeechRightPointAnimation();
    this.robotLeftControlView.stopAutoMove();
    this.robotRightControlView.turnRightAutoMove();
  }
  
  public void speechControlStartAutoCharge()
  {
    stopSpeechPointAnimation();
    if (this.speechPointImageView.getVisibility() == 0) {
      this.speechPointImageView.setVisibility(8);
    }
    startAutoCharge();
  }
  
  public void speechControlStopAutoCharge()
  {
    stopSpeechPointAnimation();
    if (this.speechPointImageView.getVisibility() == 0) {
      this.speechPointImageView.setVisibility(8);
    }
    stopAutoCharge();
  }
  
  public void speechControlUpHead()
  {
    sendRobotOrder("X5");
    stopSpeechPointAnimation();
    startSpeechUpHeadPointAnimation();
  }
  
  public void speechRegcognizSuccess(String paramString)
  {
    if (this.isInTouchControl) {
      return;
    }
    if (StringUtils.isEmpty(paramString))
    {
      if (this.speechPointImageView.getVisibility() != 0) {
        this.speechPointImageView.setVisibility(0);
      }
      this.speechPointImageView.setBackgroundResource(2130837654);
      prepareDismissSpeechPoint(1500);
      return;
    }
    paramString = SpeechRecognizeTransUtils.getRobotOrderWithTextResult(paramString);
    if (this.speechPointImageView.getVisibility() != 0) {
      this.speechPointImageView.setVisibility(0);
    }
    this.isInSpeechControl = true;
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_STOP_ORDER == paramString)
    {
      this.isInSpeechControl = false;
      stopSpeechControlMove();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_FORWARD_ORDER == paramString)
    {
      speechControlForward();
      prepareSpeechControlStopOrder(1500);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_LEFT_ORDER == paramString)
    {
      speechControlLeft();
      prepareSpeechControlStopOrder(800);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_RIGHT_ORDER == paramString)
    {
      speechControlRight();
      prepareSpeechControlStopOrder(800);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_BACKWARD_ORDER == paramString)
    {
      speechControlBack();
      prepareSpeechControlStopOrder(1500);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_HEAD_UP_ORDER == paramString)
    {
      speechControlUpHead();
      prepareSpeechControlStopOrder(1000);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_HEAD_DOWN_ORDER == paramString)
    {
      speechControlDownHead();
      prepareSpeechControlStopOrder(1000);
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_KEEP_FORWARD_ORDER == paramString)
    {
      speechControlForward();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_KEEP_LEFT_ORDER == paramString)
    {
      speechControlLeft();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_KEEP_RIGHT_ORDER == paramString)
    {
      speechControlRight();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_KEEP_BACKWARD_ORDER == paramString)
    {
      speechControlBack();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_START_AUTO_CHARGE_ORDER == paramString)
    {
      speechControlStartAutoCharge();
      return;
    }
    if (PadBotConstants.SPEECH_ROBOT_ORDER.SPEECH_ROBOT_STOP_AUTO_CHARGE_ORDER == paramString)
    {
      speechControlStopAutoCharge();
      return;
    }
    this.isInSpeechControl = false;
    stopSpeechControlMove();
  }
  
  public void speechRegcognizeEnd()
  {
    stopSpeechListeningAnimation();
  }
  
  public void speechRegcognizeError(String paramString)
  {
    stopSpeechListeningAnimation();
    if (!this.isInTouchControl)
    {
      if (this.speechPointImageView.getVisibility() != 0) {
        this.speechPointImageView.setVisibility(0);
      }
      this.speechPointImageView.setBackgroundResource(2130837654);
      prepareDismissSpeechPoint(1500);
    }
  }
  
  public void speechRegcognizeStart()
  {
    startSpeechListeningAnimation();
  }
  
  public void startSearchChargeAnimation()
  {
    if (this.chargeImageView.getVisibility() == 0) {
      this.chargeImageView.setVisibility(4);
    }
    if (this.notChargeImageView.getVisibility() == 0) {
      this.notChargeImageView.setVisibility(4);
    }
    if (this.robotThirdImageView.getVisibility() == 0) {
      this.robotThirdImageView.setVisibility(4);
    }
    if (this.cartoonBalteryImageView.getVisibility() == 0) {
      this.cartoonBalteryImageView.setVisibility(4);
    }
    if (this.robotFirstImageView.getVisibility() != 0) {
      this.robotFirstImageView.setVisibility(0);
    }
    if (this.searchChargeAnimation == null)
    {
      this.searchChargeAnimation = new TranslateAnimation(0.0F, UnitConversion.adjustLayoutSize(this, this.robotChargeLayoutWidth - this.robotMarginLeftRight * 2 - this.robotViewWidth), 0.0F, 0.0F);
      this.searchChargeAnimation.setRepeatCount(-1);
      this.searchChargeAnimation.setRepeatMode(1);
      this.searchChargeAnimation.setFillBefore(false);
      this.searchChargeAnimation.setFillAfter(true);
      this.searchChargeAnimation.setDuration(2000L);
      this.searchChargeAnimation.setAnimationListener(new Animation.AnimationListener()
      {
        public void onAnimationEnd(Animation paramAnonymousAnimation)
        {
          PHRobotControlActivity.this.isSearchChargeAnimationRunning = false;
        }
        
        public void onAnimationRepeat(Animation paramAnonymousAnimation)
        {
          Log.i("test_animation", "robotFirstImageView visibility : " + PHRobotControlActivity.this.robotFirstImageView.getVisibility());
        }
        
        public void onAnimationStart(Animation paramAnonymousAnimation)
        {
          PHRobotControlActivity.this.isSearchChargeAnimationRunning = true;
        }
      });
    }
    if (!this.isSearchChargingAnimationRunning)
    {
      this.robotFirstImageView.startAnimation(this.searchChargeAnimation);
      this.isSearchChargingAnimationRunning = true;
    }
  }
  
  public void stopSearchChargeAnimation()
  {
    this.isSearchChargingAnimationRunning = false;
    if ((this.searchChargeAnimation != null) && (this.isSearchChargeAnimationRunning))
    {
      this.robotFirstImageView.clearAnimation();
      this.searchChargeAnimation.cancel();
    }
  }
  
  public void stopSpeechControlMove()
  {
    sendRobotOrder("0");
    stopSpeechPointAnimation();
    this.speechPointImageView.setBackgroundResource(2130837653);
    prepareDismissSpeechPoint(1500);
    this.robotLeftControlView.stopAutoMove();
    this.robotRightControlView.stopAutoMove();
  }
  
  private class GuidePageAdapter
    extends PagerAdapter
  {
    private GuidePageAdapter() {}
    
    public void destroyItem(View paramView, int paramInt, Object paramObject)
    {
      ((ViewPager)paramView).removeView((View)PHRobotControlActivity.this.pointPageViewList.get(paramInt));
    }
    
    public void finishUpdate(View paramView) {}
    
    public int getCount()
    {
      return PHRobotControlActivity.this.pointPageViewList.size();
    }
    
    public int getItemPosition(Object paramObject)
    {
      return super.getItemPosition(paramObject);
    }
    
    public Object instantiateItem(View paramView, int paramInt)
    {
      View localView = (View)PHRobotControlActivity.this.pointPageViewList.get(paramInt);
      localView.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          PHRobotControlActivity.this.setPointViewInvisible();
        }
      });
      ((ViewPager)paramView).addView(localView);
      return localView;
    }
    
    public boolean isViewFromObject(View paramView, Object paramObject)
    {
      return paramView == paramObject;
    }
    
    public void restoreState(Parcelable paramParcelable, ClassLoader paramClassLoader) {}
    
    public Parcelable saveState()
    {
      return null;
    }
    
    public void startUpdate(View paramView) {}
  }
  
  class GuidePageChangeListener
    implements ViewPager.OnPageChangeListener
  {
    GuidePageChangeListener() {}
    
    public void onPageScrollStateChanged(int paramInt) {}
    
    public void onPageScrolled(int paramInt1, float paramFloat, int paramInt2) {}
    
    public void onPageSelected(int paramInt)
    {
      int i = 0;
      for (;;)
      {
        if (i >= PHRobotControlActivity.this.pointCursorImageViews.length) {
          return;
        }
        PHRobotControlActivity.this.pointCursorImageViews[paramInt].setBackgroundResource(2130837633);
        if (paramInt != i) {
          PHRobotControlActivity.this.pointCursorImageViews[i].setBackgroundResource(2130837632);
        }
        i += 1;
      }
    }
  }
}


/* Location:              /Users/fdhuang/pabot/classes-dex2jar.jar!/cn/inbot/padbotphone/robot/PHRobotControlActivity.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */