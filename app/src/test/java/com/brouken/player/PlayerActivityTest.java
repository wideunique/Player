package com.brouken.player;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.media3.common.Player;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlayerActivityTest {

    @Mock
    private Player player;

    @Mock
    private CustomPlayerView playerView;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void restoreControllerAfterPlaybackEnded_showsControlsAndPausesPlayback() {
        Runnable barsHider = mock(Runnable.class);

        PlayerActivity.restoreControllerAfterPlaybackEnded(player, playerView, barsHider);

        verify(player).setPlayWhenReady(false);
        verify(playerView).setControllerShowTimeoutMs(-1);
        verify(playerView).removeCallbacks(barsHider);
        verify(playerView).showController();
    }

    @Test
    public void restoreControllerAfterPlaybackEnded_handlesNullInputs() {
        PlayerActivity.restoreControllerAfterPlaybackEnded(null, null, null);
    }
}
