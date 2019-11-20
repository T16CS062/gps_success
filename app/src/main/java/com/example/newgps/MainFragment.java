package com.example.newgps;
import android.os.Bundle;
// 下位のバージョンにも対応させる場合はsupport-v4パッケージを使用します
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// Fragmentクラスを継承します
public class MainFragment extends Fragment {

    private TextView mTextView;

    // Fragmentで表示するViewを作成するメソッド
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // 先ほどのレイアウトをここでViewとして作成します
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    // Viewが生成し終わった時に呼ばれるメソッド
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TextViewをひも付けます
        mTextView = (TextView) view.findViewById(R.id.text_frag_gyro);
        // Buttonのクリックした時の処理を書きます
        view.findViewById(R.id.frag_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextView.setText(mTextView.getText() + "!");
            }
        });
    }
}
