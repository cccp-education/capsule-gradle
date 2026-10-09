import React from 'react';
import { Composition } from 'remotion';
import { Capsule } from './Capsule.jsx';

/**
 * Root composition of the capsule template.
 *
 * Dimensions, frame rate and length come from the props document written by the
 * plugin (`capsule-props.json`). They are resolved by `calculateMetadata`, so a
 * selected composition already carries the exact shape the plugin planned — the
 * legacy landscape default for a pedagogical capsule, or the vertical 9:16
 * 1080×1920 preset for a viral campaign (`capsule.viral.ViralViewport`).
 *
 * The placeholders below only exist so the composition can be selected before
 * the real props are known; they are overwritten by `calculateMetadata`.
 */
export const Root = () => (
  <Composition
    id="Capsule"
    component={Capsule}
    durationInFrames={300}
    fps={30}
    width={1920}
    height={1080}
    defaultProps={{ slides: [], headHtml: '', fps: 30 }}
    calculateMetadata={({ props }) => ({
      width: props.width ?? 1920,
      height: props.height ?? 1080,
      fps: props.fps ?? 30,
      durationInFrames: props.totalFrames ?? 300,
    })}
  />
);
